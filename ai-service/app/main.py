import os
from typing import Annotated

from fastapi import Depends, FastAPI, Header, HTTPException

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


def require_internal_key(x_stockpilot_internal_key: Annotated[str | None, Header()] = None) -> None:
    expected_key = os.getenv("AI_INTERNAL_API_KEY")
    if expected_key and x_stockpilot_internal_key != expected_key:
        raise HTTPException(status_code=401, detail="Invalid internal API key")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/internal/decisions/generate", response_model=DecisionResponse, dependencies=[Depends(require_internal_key)])
def generate(request: DecisionRequest) -> DecisionResponse:
    return generate_decisions(request)


@app.post("/internal/copilot/answer", response_model=CopilotResponse, dependencies=[Depends(require_internal_key)])
def copilot(request: CopilotRequest) -> CopilotResponse:
    return answer_copilot(request)


@app.post("/internal/recommendations/explain", response_model=RecommendationExplanationResponse, dependencies=[Depends(require_internal_key)])
def explain(request: RecommendationExplanationRequest) -> RecommendationExplanationResponse:
    return explain_recommendation(request)
