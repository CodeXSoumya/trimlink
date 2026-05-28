import { useMemo, useState } from "react";
import { ShortenForm } from "../features/shortener/components/ShortenForm";
import { ShortLinkCard } from "../features/shortener/components/ShortLinkCard";
import { getHistory } from "../features/shortener/hooks/useShortenUrl";
import { ShortenResponse } from "../features/shortener/types";
import { AppShell } from "../shared/components/AppShell";

export function HomePage() {
  const [latest, setLatest] = useState<ShortenResponse | null>(null);
  const [refreshTick, setRefreshTick] = useState(0);

  const history = useMemo(() => getHistory(), [refreshTick, latest]);

  const handleCreated = (item: ShortenResponse) => {
    setLatest(item);
    setRefreshTick((v) => v + 1);
  };

  return (
    <AppShell>
      <section className="hero reveal">
        <p className="eyebrow">Production Frontend</p>
        <h1>Ship links at edge speed with a clean operational UI.</h1>
        <p>
          This React app integrates with the gateway API, carries client-based rate-limit identity,
          validates input aggressively, and gives immediate operational feedback.
        </p>
      </section>

      <div className="gridLayout">
        <ShortenForm onCreated={handleCreated} />

        <section className="stack">
          <h2>Latest Result</h2>
          {latest ? <ShortLinkCard link={latest} /> : <p className="muted">No link created yet.</p>}
        </section>
      </div>

      <section className="stack">
        <h2>Recent Links</h2>
        {history.length === 0 ? (
          <p className="muted">History will appear here after you generate links.</p>
        ) : (
          <div className="historyGrid">
            {history.map((entry) => (
              <ShortLinkCard key={entry.shortCode} link={entry} />
            ))}
          </div>
        )}
      </section>
    </AppShell>
  );
}
