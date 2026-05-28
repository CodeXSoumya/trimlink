import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { useShortenUrl } from "../hooks/useShortenUrl";
import { ApiError, ShortenResponse } from "../types";

const schema = z.object({
  longUrl: z
    .string()
    .trim()
    .url("Enter a valid URL including protocol, for example https://example.com")
});

type FormData = z.infer<typeof schema>;

type Props = {
  onCreated: (item: ShortenResponse) => void;
};

export function ShortenForm({ onCreated }: Props) {
  const [apiError, setApiError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      longUrl: ""
    }
  });

  const mutation = useShortenUrl((item) => {
    setApiError(null);
    onCreated(item);
    reset();
  });

  const submit = handleSubmit(async (values) => {
    try {
      await mutation.mutateAsync(values);
    } catch (error) {
      const typedError = error as ApiError;
      if (typedError.status === 429) {
        setApiError("Rate limit exceeded. Wait a minute and retry.");
        return;
      }
      setApiError(typedError.message || "Failed to shorten URL");
    }
  });

  return (
    <section className="card reveal" aria-label="URL shortener form">
      <div className="cardHeader">
        <h2>Create a short link</h2>
        <p>Paste a full URL and generate a redirect token through the distributed backend.</p>
      </div>

      <form onSubmit={submit} className="formGrid" noValidate>
        <label htmlFor="longUrl">Long URL</label>
        <input
          id="longUrl"
          type="url"
          placeholder="https://example.com/some/path"
          autoComplete="url"
          {...register("longUrl")}
        />
        {errors.longUrl ? <p className="inputError">{errors.longUrl.message}</p> : null}

        {apiError ? <p className="apiError">{apiError}</p> : null}

        <button disabled={mutation.isPending} type="submit" className="primaryButton">
          {mutation.isPending ? "Generating..." : "Generate Short Link"}
        </button>
      </form>
    </section>
  );
}
