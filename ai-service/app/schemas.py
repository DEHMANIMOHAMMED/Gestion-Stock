from datetime import date, datetime
from decimal import Decimal
from typing import Literal

from pydantic import BaseModel, Field
from pydantic import ConfigDict


MovementType = Literal["IN", "OUT", "ADJUST"]
RiskLevel = Literal["LOW", "MEDIUM", "HIGH"]


def to_camel(value: str) -> str:
    parts = value.split("_")
    return parts[0] + "".join(part.capitalize() for part in parts[1:])


class ApiModel(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)


class ProductSnapshot(ApiModel):
    id: int
    name: str
    sku: str
    category: str | None = None
    min_stock: int = Field(ge=0)
    unit: str | None = None


class StockSnapshot(ApiModel):
    product_id: int
    quantity: int = Field(ge=0)


class MovementSnapshot(ApiModel):
    id: int
    product_id: int
    quantity: int = Field(gt=0)
    type: MovementType
    created_at: datetime


class DecisionRequest(ApiModel):
    organisation_id: int
    products: list[ProductSnapshot]
    stocks: list[StockSnapshot]
    movements: list[MovementSnapshot]
    horizons: list[int] = [7, 30, 90]
    lead_time_days: int = Field(default=7, ge=1, le=365)


class ForecastDecision(ApiModel):
    product_id: int
    horizon_days: int
    predicted_quantity: Decimal
    confidence_score: Decimal
    model_name: str


class StockoutRiskDecision(ApiModel):
    product_id: int
    estimated_stockout_date: date | None
    risk_score: Decimal
    risk_level: RiskLevel
    reason: str


class ReorderDecision(ApiModel):
    product_id: int
    recommended_quantity: int
    lead_time_days: int
    safety_stock: int
    reason: str
    status: str = "PENDING"


class AnomalyDecision(ApiModel):
    product_id: int | None
    stock_movement_id: int | None
    anomaly_type: str
    severity: RiskLevel
    score: Decimal
    explanation: str


class InsightDecision(ApiModel):
    title: str
    content: str
    insight_type: str
    priority: RiskLevel


class DecisionResponse(ApiModel):
    forecasts: list[ForecastDecision]
    stockout_risks: list[StockoutRiskDecision]
    reorder_recommendations: list[ReorderDecision]
    anomalies: list[AnomalyDecision]
    insights: list[InsightDecision]


class CopilotContextItem(ApiModel):
    type: str
    title: str
    content: str
    product_id: int | None = None
    supplier_id: int | None = None
    purchase_order_id: int | None = None


class CopilotCitation(ApiModel):
    type: str
    label: str
    product_id: int | None = None
    supplier_id: int | None = None
    purchase_order_id: int | None = None


class CopilotRequest(ApiModel):
    organisation_id: int
    question: str = Field(min_length=1, max_length=800)
    context_items: list[CopilotContextItem]


class CopilotResponse(ApiModel):
    answer: str
    bullets: list[str]
    related_product_ids: list[int]
    citations: list[CopilotCitation] = []
    source: str


class RecommendationExplanationRequest(ApiModel):
    organisation_id: int
    recommendation_id: int
    product_name: str
    sku: str
    recommended_quantity: int
    current_stock: int
    min_stock: int
    average_daily_demand: Decimal
    stock_coverage_days: Decimal
    risk_score: Decimal
    risk_level: str
    supplier_name: str | None = None
    supplier_score: Decimal | None = None
    supplier_explanation: str | None = None
    reason: str


class RecommendationExplanationResponse(ApiModel):
    recommendation_id: int
    summary: str
    drivers: list[str]
    risks: list[str]
    next_action: str
    source: str
