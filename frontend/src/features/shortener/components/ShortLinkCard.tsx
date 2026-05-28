import { ShortenResponse } from "../types";
import { resolveShortCodeUrl } from "../../../lib/api/trimlink";

type Props = {
  link: ShortenResponse;
};

export function ShortLinkCard({ link }: Props) {
  const resolveUrl = resolveShortCodeUrl(link.shortCode);

  const copy = async () => {
    await navigator.clipboard.writeText(resolveUrl);
  };

  return (
    <article className="linkCard reveal">
      <div>
        <p className="eyebrow">Short code</p>
        <h3>{link.shortCode}</h3>
        <a href={resolveUrl} target="_blank" rel="noreferrer">
          {resolveUrl}
        </a>
      </div>
      <div className="linkCardActions">
        <button onClick={copy} className="ghostButton" type="button">
          Copy
        </button>
        <a className="primaryButton" href={resolveUrl} target="_blank" rel="noreferrer">
          Open
        </a>
      </div>
    </article>
  );
}
