# Receiving messages from Barid

Barid forwards bank transaction SMS to one HTTP(S) endpoint. This page describes what that endpoint receives and how a server can hand Barid its settings in a QR code. Version 1.

## Connecting a phone

There are three ways to connect Barid to a server:

- **Scan a setup code.** The server shows a QR code that holds the setup code.
- **Open a pairing link.** For example, a button on the server's page.
- **Enter the settings by hand.** In the app: Connect → Use another server.

Before Barid connects to any server other than `darakav.com`, it shows the server's host name and asks the owner to confirm. A scanned code alone can't send messages somewhere the owner didn't choose.

### Setup code

```
barid:1:<base64url(JSON)>
```

The base64url encoding has no padding (padding is accepted). The JSON object holds:

| Field | Required | Meaning |
|---|---|---|
| `u` | yes | Endpoint URL, `https://` (or `http://` for testing on a local network). |
| `k` | no | Key or token sent with every request, up to 1,024 characters. |
| `h` | no | Header that carries `k`. When it's missing, Barid sends `Authorization: Bearer <k>`. When it's set (for example `X-Api-Key`), the header's value is `k` exactly. |
| `n` | no | A label shown on the phone, up to 64 characters. |
| `t` | no | A body template (see below). When it's missing, Barid sends its JSON batch. |

Barid ignores unknown fields, so later versions can add fields without breaking older apps.

### Pairing link

```
barid://pair?c=<URL-encoded setup code>
```

Opening this link on the phone opens Barid with the setup code filled in. Darakav uses its own `darakav-sms:1:` setup codes and `darakav://pair` links, which Barid also reads.

## Requests

Every request is a `POST` to `u` with these headers:

- `User-Agent: Barid/<version>`
- `Accept: application/json`
- `Content-Type` (see below)
- the key header, when a key is set.

Barid doesn't follow redirects.

### JSON batch (default)

The body holds up to five messages per request, oldest first:

```json
{"messages":[{"from":"BankMellat","text":"…","receivedStamp":1762850000000}]}
```

- `from` is the SMS sender.
- `text` is the full message, with multipart messages joined.
- `receivedStamp` is the time the phone received the message, in milliseconds since the Unix epoch.

When the owner taps *Test connection*, Barid sends `{"messages":[]}`. The server should accept it with a 2xx response.

### Template

With a template, Barid sends **one request per message**, and the body is the template with these placeholders filled in:

| Placeholder | Value |
|---|---|
| `{{from}}` | sender |
| `{{text}}` | message text |
| `{{receivedStamp}}` | milliseconds since the Unix epoch |
| `{{receivedAt}}` | ISO 8601 time with the phone's UTC offset, for example `2025-11-11T12:03:20+03:30` |

A template must contain `{{text}}` and be at most 4,000 characters.

A template is sent as JSON when it starts with `{` or `[`, but not when it starts with a placeholder such as `{{from}}`. In a JSON template, values are escaped as JSON string content, so write the placeholders inside quotes:

```json
{"from":"{{from}}","text":"{{text}}","receivedAt":"{{receivedAt}}"}
```

These are sent with `Content-Type: application/json; charset=utf-8`. Any other template is sent as `text/plain; charset=utf-8`, with the values inserted as they are. Placeholders that appear inside a message's own text are never expanded.

When the owner taps *Send a test*, Barid sends one message from `Barid` with the text "Test message from Barid".

## Responses

| Status | Barid's response |
|---|---|
| 2xx | Delivered. The messages are removed from the phone's queue. |
| 401, 403 | The key was refused. Barid stops forwarding and asks the owner to connect again. |
| 400, 413, 422 | This content was refused. Barid resends a refused batch one message at a time, and drops any single message the server still refuses. |
| anything else, or no response | Barid tries again later, with exponential backoff. |

Barid ignores the response body.

## What servers should do

- **Ignore repeats.** A request can arrive twice, for example when the response was lost. A message is identified by `from`, `text` and `receivedStamp`.
- **Answer quickly.** Barid gives up after 15 seconds to connect and 20 seconds to read the response.
- **Use HTTPS.** Message text is financial data. Barid warns the owner whenever a connection uses plain HTTP.

## What Barid never sends

- One-time passwords and verification codes.
- Messages from personal mobile numbers.
- Messages without banking words and an amount. The rules are in `SmsFilter.kt`.
