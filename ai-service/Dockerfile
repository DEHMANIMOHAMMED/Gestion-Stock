FROM python:3.12-slim

WORKDIR /app
RUN useradd --create-home --shell /usr/sbin/nologin stockpilot

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app
RUN chown -R stockpilot:stockpilot /app
USER stockpilot

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
