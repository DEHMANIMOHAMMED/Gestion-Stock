import json
import os

from app.schemas import (
    CopilotCitation,
    CopilotRequest,
    CopilotResponse,
    RecommendationExplanationRequest,
    RecommendationExplanationResponse,
)


def answer_copilot(request: CopilotRequest) -> CopilotResponse:
    prompt = {
        "question": request.question,
        "context_items": [item.model_dump(mode="json") for item in request.context_items[:40]],
    }
    fallback = _fallback_copilot(request)
    text = _call_openai(
        instructions=(
            "Tu es StockPilot AI, un copilote SaaS de gestion de stock pour TPE/PME. "
            "Reponds uniquement avec les donnees fournies. N'invente pas de donnees hors contexte. "
            "Retourne un JSON strict avec answer, bullets, related_product_ids."
        ),
        payload=prompt,
    )
    if not text:
        return fallback
    try:
        data = json.loads(text)
        return CopilotResponse(
            answer=str(data.get("answer") or fallback.answer),
            bullets=[str(item) for item in data.get("bullets", [])][:6] or fallback.bullets,
            related_product_ids=[int(item) for item in data.get("related_product_ids", [])][:8],
            citations=_citations_from_context(request.context_items[:6]),
            source="OPENAI",
        )
    except (TypeError, ValueError, json.JSONDecodeError):
        return fallback


def explain_recommendation(request: RecommendationExplanationRequest) -> RecommendationExplanationResponse:
    fallback = _fallback_explanation(request)
    text = _call_openai(
        instructions=(
            "Tu expliques une recommandation de reapprovisionnement. "
            "Reste factuel, concis, actionnable, sans inventer de fournisseur ni de chiffres. "
            "Retourne un JSON strict avec summary, drivers, risks, next_action."
        ),
        payload=request.model_dump(mode="json"),
    )
    if not text:
        return fallback
    try:
        data = json.loads(text)
        return RecommendationExplanationResponse(
            recommendation_id=request.recommendation_id,
            summary=str(data.get("summary") or fallback.summary),
            drivers=[str(item) for item in data.get("drivers", [])][:6] or fallback.drivers,
            risks=[str(item) for item in data.get("risks", [])][:5] or fallback.risks,
            next_action=str(data.get("next_action") or fallback.next_action),
            source="OPENAI",
        )
    except (TypeError, ValueError, json.JSONDecodeError):
        return fallback


def _call_openai(instructions: str, payload: dict) -> str | None:
    if not os.getenv("OPENAI_API_KEY"):
        return None
    try:
        from openai import OpenAI

        client = OpenAI()
        response = client.responses.create(
            model=os.getenv("OPENAI_MODEL", "gpt-5"),
            instructions=instructions,
            input=json.dumps(payload, ensure_ascii=False),
        )
        return response.output_text
    except Exception:
        return None


def _fallback_copilot(request: CopilotRequest) -> CopilotResponse:
    product_ids = [item.product_id for item in request.context_items if item.product_id is not None]
    top_items = request.context_items[:5]
    bullets = [f"{item.title}: {item.content}" for item in top_items]
    return CopilotResponse(
        answer="Synthese StockPilot AI basee sur les donnees du tenant courant.",
        bullets=bullets or ["Aucun signal critique dans le contexte disponible."],
        related_product_ids=list(dict.fromkeys(product_ids))[:8],
        citations=_citations_from_context(top_items),
        source="LOCAL_FALLBACK",
    )


def _citations_from_context(items) -> list[CopilotCitation]:
    return [
        CopilotCitation(
            type=item.type,
            label=item.title,
            product_id=item.product_id,
            supplier_id=item.supplier_id,
            purchase_order_id=item.purchase_order_id,
        )
        for item in items
    ]


def _fallback_explanation(request: RecommendationExplanationRequest) -> RecommendationExplanationResponse:
    drivers = [
        f"Stock actuel {request.current_stock} pour un seuil minimum {request.min_stock}.",
        f"Demande moyenne {request.average_daily_demand} unite/jour et couverture estimee {request.stock_coverage_days} jours.",
        f"Quantite recommandee {request.recommended_quantity} unite(s).",
    ]
    if request.supplier_name:
        drivers.append(f"Fournisseur retenu: {request.supplier_name}, score {request.supplier_score}.")
    risks = []
    if request.risk_level == "HIGH":
        risks.append("Risque de rupture eleve si la commande n'est pas traitee rapidement.")
    if request.stock_coverage_days < 7:
        risks.append("Couverture inferieure a une semaine.")
    if not request.supplier_name:
        risks.append("Aucun fournisseur prioritaire n'est disponible dans la recommandation.")
    return RecommendationExplanationResponse(
        recommendation_id=request.recommendation_id,
        summary=f"Commander {request.recommended_quantity} unite(s) de {request.product_name} pour couvrir le risque {request.risk_level}.",
        drivers=drivers,
        risks=risks or ["Aucun risque secondaire majeur detecte."],
        next_action="Transformer la recommandation en commande fournisseur brouillon, puis valider le fournisseur et la quantite.",
        source="LOCAL_FALLBACK",
    )
