alter table ai_forecasts add column selected_model varchar(80);
alter table ai_forecasts add column model_selection_reason text;
alter table ai_forecasts add column moving_average_error numeric(8, 2);
alter table ai_forecasts add column seasonal_error numeric(8, 2);
alter table ai_forecasts add column fastapi_error numeric(8, 2);

alter table ai_forecast_snapshots add column selected_model varchar(80);
alter table ai_forecast_snapshots add column model_selection_reason text;
alter table ai_forecast_snapshots add column moving_average_error numeric(8, 2);
alter table ai_forecast_snapshots add column seasonal_error numeric(8, 2);
alter table ai_forecast_snapshots add column fastapi_error numeric(8, 2);
