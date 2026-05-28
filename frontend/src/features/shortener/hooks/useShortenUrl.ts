import { useMutation } from "@tanstack/react-query";
import { createShortCode } from "../../../lib/api/trimlink";
import { ShortenResponse } from "../types";

const HISTORY_KEY = "trimlink-history";
const MAX_HISTORY = 8;

function saveHistory(item: ShortenResponse) {
  const existing = getHistory();
  const deduped = existing.filter((entry) => entry.shortCode !== item.shortCode);
  const next = [item, ...deduped].slice(0, MAX_HISTORY);
  localStorage.setItem(HISTORY_KEY, JSON.stringify(next));
}

export function getHistory(): ShortenResponse[] {
  const raw = localStorage.getItem(HISTORY_KEY);
  if (!raw) {
    return [];
  }

  try {
    const parsed = JSON.parse(raw) as ShortenResponse[];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function useShortenUrl(onSuccess: (item: ShortenResponse) => void) {
  return useMutation({
    mutationFn: createShortCode,
    onSuccess: (item) => {
      saveHistory(item);
      onSuccess(item);
    }
  });
}
