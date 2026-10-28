# 2. Partial results: leave out similar products that cannot be fetched

[Español](0002-partial-results.es.md)

- Status: accepted
- Date: 2026-10-28

## Context

The contract only defines `200` (list of similar products) and `404` (product not found). The mocks return 404, 500
or very slow answers for some similar products. Returning an error for the whole list because one similar product
failed would make the feature fragile.

## Decision

- A similar product whose detail returns 404, 5xx, times out or is blocked by an open circuit or a full bulkhead is
  **left out**; the response is `200` with the remaining products, still in similarity order.
- A detail that lacks a field the contract requires (`id`, `name`, `price`, `availability`) is also left out, with a
  warning in the log: forwarding it would break the contract for our own clients.
- `404` is returned only when the **requested** product does not exist (the similar-ids call returns 404).
- If the similar ids cannot be fetched (5xx, timeout, open circuit), the response is `503` with an RFC 9457
  Problem Detail. This status is outside the agreed contract, but without ids there is nothing meaningful to return,
  and 503 tells the caller the failure is temporary.
- Duplicate ids are removed keeping their first position (the contract declares `uniqueItems`).

## Consequences

- The "notFound" and "error" load-test scenarios answer `200` with two products instead of failing.
- A client cannot tell from the response that a product was left out. It shows up in the
  `similar.products.left.out` metric, tagged `missing` (not found or incomplete) or `failed` (error, timeout, open
  circuit, full bulkhead), and in the logs.
- A `503` is logged with its cause, since the client only receives a generic message.
