import { APP_CONFIG } from '../../config/env';
import { ApiResponse, ApiError } from '../../types/api';

export class ApiClientError extends Error {
  public readonly errorCode: string;
  public readonly statusCode: number;
  public readonly timestamp: string;

  constructor(error: ApiError, statusCode: number) {
    super(error.message || 'An unknown API error occurred.');
    this.name = 'ApiClientError';
    this.errorCode = error.errorCode || 'UNKNOWN_ERROR';
    this.statusCode = statusCode;
    this.timestamp = error.timestamp || new Date().toISOString();
  }
}

export async function apiFetch<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const url = `${APP_CONFIG.apiBaseUrl}${endpoint}`;
  const defaultHeaders: Record<string, string> = {
    'Accept': 'application/json',
  };

  if (options.body && typeof options.body === 'string') {
    defaultHeaders['Content-Type'] = 'application/json';
  }

  const response = await fetch(url, {
    ...options,
    headers: {
      ...defaultHeaders,
      ...(options.headers as Record<string, string>),
    },
  });

  const text = await response.text();
  let json: unknown;
  try {
    json = text ? JSON.parse(text) : {};
  } catch {
    json = { message: text };
  }

  if (!response.ok) {
    const errorData = json as Partial<ApiError>;
    throw new ApiClientError(
      {
        success: false,
        errorCode: errorData.errorCode || `HTTP_${response.status}`,
        message: errorData.message || response.statusText,
        timestamp: errorData.timestamp || new Date().toISOString(),
      },
      response.status
    );
  }

  const apiRes = json as ApiResponse<T>;
  return apiRes.data !== undefined ? apiRes.data : (json as T);
}