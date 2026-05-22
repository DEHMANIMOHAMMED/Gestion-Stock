export interface StockPilotRuntimeConfig {
  googleClientId?: string;
}

declare global {
  interface Window {
    stockPilotConfig?: StockPilotRuntimeConfig;
  }
}

export function runtimeGoogleClientId(): string {
  return window.stockPilotConfig?.googleClientId?.trim() ?? '';
}
