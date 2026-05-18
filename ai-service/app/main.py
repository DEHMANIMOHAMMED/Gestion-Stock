from fastapi import FastAPI

from app.schemas import (
    CopilotRequest,
    CopilotResponse,
    DecisionRequest,
    DecisionResponse,
    RecommendationExplanationRequest,
    RecommendationExplanationResponse,
)
from app.services.decision_engine import generate_decisions
from app.services.openai_copilot import answer_copilot, explain_recommendation

app = FastAPI(title="StockPilot AI Engine", version="0.1.0")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/internal/decisions/generate", response_model=DecisionResponse)
def generate(request: DecisionRequest) -> DecisionResponse:
    return generate_decisions(request)


@app.post("/internal/copilot/answer", response_model=CopilotResponse)
def copilot(request: CopilotRequest) -> CopilotResponse:
    return answer_copilot(request)


@app.post("/internal/recommendations/explain", response_model=RecommendationExplanationResponse)
def explain(request: RecommendationExplanationRequest) -> RecommendationExplanationResponse:
    return explain_recommendation(request)
