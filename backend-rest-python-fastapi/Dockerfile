FROM python:3.12-slim

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY src ./src
ENV PYTHONPATH=/app/src
ENV SUT_PORT=8080
EXPOSE 8080
CMD ["python", "-m", "uvicorn", "order_api.app:app", "--host", "0.0.0.0", "--port", "8080"]
