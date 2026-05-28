import { AxiosError } from "axios";
import {
  ApiError,
  ShortenRequest,
  ShortenResponse
} from "../../features/shortener/types";
import { apiBaseUrl, http } from "./http";

function toApiError(error: unknown): ApiError {
  if (error instanceof AxiosError) {
    return {
      message:
        typeof error.response?.data === "string"
          ? error.response.data
          : error.message,
      status: error.response?.status
    };
  }
  return { message: "Unexpected error" };
}

export async function createShortCode(
  payload: ShortenRequest
): Promise<ShortenResponse> {
  try {
    const response = await http.post<string>(
      "/shorten",
      null,
      {
        params: { longUrl: payload.longUrl }
      }
    );

    return {
      shortCode: response.data,
      longUrl: payload.longUrl,
      createdAt: new Date().toISOString()
    };
  } catch (error) {
    throw toApiError(error);
  }
}

export function resolveShortCodeUrl(shortCode: string): string {
  return `${apiBaseUrl}/resolve/${encodeURIComponent(shortCode)}`;
}
