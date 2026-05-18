from collections import defaultdict
from datetime import date, datetime, timedelta, timezone
from decimal import Decimal, ROUND_HALF_UP

from app.schemas import (
    AnomalyDecision,
    DecisionRequest,
    DecisionResponse,
    ForecastDecision,
    InsightDecision,
    MovementSnapshot,
    ProductSnapshot,
    ReorderDecision,
    StockoutRiskDecision,
)


def generate_decisions(request: DecisionRequest) -> DecisionResponse:
    movements_by_product: dict[int, list[MovementSnapshot]] = defaultdict(list)
    stock_by_product = {stock.product_id: stock.quantity for stock in request.stocks}

    for movement in request.movements:
        movements_by_product[movement.product_id].append(movement)

    forecasts: list[ForecastDecision] = []
    risks: list[StockoutRiskDecision] = []
    recommendations: list[ReorderDecision] = []

    for product in request.products:
        current_stock = stock_by_product.get(product.id, 0)
        demand = daily_demand(movements_by_product[product.id])

        for horizon in request.horizons:
            forecasts.append(
                ForecastDecision(
                    product_id=product.id,
                    horizon_days=horizon,
                    predicted_quantity=money(demand * horizon),
                    confidence_score=money(72 if demand > 0 else 55),
                    model_name="fastapi-moving-average-mvp",
                )
            )

        risks.append(stockout_risk(product, current_stock, demand))

        recommendation = reorder_recommendation(product, current_stock, demand, request.lead_time_days)
        if recommendation.recommended_quantity > 0:
            recommendations.append(recommendation)

    anomalies = detect_anomalies(request.movements, movements_by_product)
    insights = build_insights(request.products, stock_by_product)

    return DecisionResponse(
        forecasts=forecasts,
        stockout_risks=sorted(risks, key=lambda item: item.risk_score, reverse=True),
        reorder_recommendations=sorted(recommendations, key=lambda item: item.recommended_quantity, reverse=True),
        anomalies=anomalies,
        insights=insights,
    )


def stockout_risk(product: ProductSnapshot, current_stock: int, demand: float) -> StockoutRiskDecision:
    min_stock = max(product.min_stock, 0)
    ratio = 0 if min_stock == 0 else max(0, min_stock - current_stock) / min_stock
    risk_score = 100 if current_stock == 0 else min(100, ratio * 85 + demand * 5)
    risk_level = "HIGH" if risk_score >= 70 else "MEDIUM" if risk_score >= 35 else "LOW"
    estimated_date = None if demand <= 0 else date.today() + timedelta(days=max(0, int(current_stock / demand)))

    return StockoutRiskDecision(
        product_id=product.id,
        estimated_stockout_date=estimated_date,
        risk_score=money(risk_score),
        risk_level=risk_level,
        reason=f"Stock actuel {current_stock}, seuil {min_stock}, demande moyenne {round(demand, 2)} unite/jour.",
    )


def reorder_recommendation(
    product: ProductSnapshot,
    current_stock: int,
    demand: float,
    lead_time_days: int,
) -> ReorderDecision:
    safety_stock = max(product.min_stock, int_ceil(demand * 3))
    lead_time_demand = int_ceil(demand * lead_time_days)
    quantity = max(0, lead_time_demand + safety_stock - current_stock)

    return ReorderDecision(
        product_id=product.id,
        recommended_quantity=quantity,
        lead_time_days=lead_time_days,
        safety_stock=safety_stock,
        reason=f"Commande basee sur lead time {lead_time_days} jours, stock {current_stock} et stock de securite {safety_stock}.",
    )


def detect_anomalies(
    movements: list[MovementSnapshot],
    movements_by_product: dict[int, list[MovementSnapshot]],
) -> list[AnomalyDecision]:
    anomalies: list[AnomalyDecision] = []

    for movement in movements:
        product_movements = movements_by_product[movement.product_id]
        average = sum(item.quantity for item in product_movements) / len(product_movements)
        is_adjust_anomaly = movement.type == "ADJUST" and movement.quantity >= 10
        is_volume_anomaly = average > 0 and movement.quantity >= max(25, average * 3)

        if is_adjust_anomaly or is_volume_anomaly:
            anomalies.append(
                AnomalyDecision(
                    product_id=movement.product_id,
                    stock_movement_id=movement.id,
                    anomaly_type="UNUSUAL_MOVEMENT",
                    severity="HIGH" if movement.quantity >= 50 else "MEDIUM",
                    score=money(min(100, movement.quantity * 2)),
                    explanation=f"Mouvement {movement.type} de {movement.quantity} unites au-dessus du comportement recent.",
                )
            )

    return anomalies[:50]


def build_insights(products: list[ProductSnapshot], stock_by_product: dict[int, int]) -> list[InsightDecision]:
    low_stock = sum(1 for product in products if stock_by_product.get(product.id, 0) <= product.min_stock)
    out_of_stock = sum(1 for product in products if stock_by_product.get(product.id, 0) == 0)

    return [
        InsightDecision(
            title="Priorite stock",
            content=f"{low_stock} produit(s) demandent une action. {out_of_stock} sont en rupture complete.",
            insight_type="STOCK_HEALTH",
            priority="HIGH" if out_of_stock else "MEDIUM" if low_stock else "LOW",
        )
    ]


def daily_demand(movements: list[MovementSnapshot]) -> float:
    out_quantity = sum(movement.quantity for movement in movements if movement.type == "OUT")
    if not movements:
        return 0.0

    oldest = min(movement.created_at for movement in movements)
    now = datetime.now(timezone.utc)
    if oldest.tzinfo is None:
        oldest = oldest.replace(tzinfo=timezone.utc)
    active_days = max(7, (now - oldest).days or 1)
    return out_quantity / active_days


def money(value: float | int) -> Decimal:
    return Decimal(str(value)).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)


def int_ceil(value: float) -> int:
    rounded = int(value)
    return rounded if rounded == value else rounded + 1
