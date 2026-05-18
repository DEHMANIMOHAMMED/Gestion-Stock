from datetime import datetime, timedelta

from app.schemas import DecisionRequest, MovementSnapshot, ProductSnapshot, StockSnapshot
from app.services.decision_engine import generate_decisions


def test_generate_decisions_returns_forecasts_risks_and_reorders():
    request = DecisionRequest(
        organisation_id=1,
        products=[
            ProductSnapshot(id=10, name="Scanner", sku="SCN-1", min_stock=10, unit="pcs"),
        ],
        stocks=[
            StockSnapshot(product_id=10, quantity=2),
        ],
        movements=[
            MovementSnapshot(
                id=100,
                product_id=10,
                quantity=14,
                type="OUT",
                created_at=datetime.now() - timedelta(days=3),
            )
        ],
    )

    response = generate_decisions(request)

    assert len(response.forecasts) == 3
    assert response.stockout_risks[0].risk_level in {"MEDIUM", "HIGH"}
    assert response.reorder_recommendations[0].recommended_quantity > 0
    assert response.insights[0].priority in {"MEDIUM", "HIGH"}
