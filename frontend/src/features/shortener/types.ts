export type ShortenRequest = {
  longUrl: string;
};

export type ShortenResponse = {
  shortCode: string;
  longUrl: string;
  createdAt: string;
};

export type ApiError = {
  message: string;
  status?: number;
};
