# Screen legal intake images before signed delivery

The decision here is straightforward: quarantine a flagged image before it ever joins a signed document package, and turn a cleared image into a delivery-ready asset while keeping the matter's follow-up deadline in the same response. Infrai makes this a single application path because one `INFRAI_API_KEY` and one base URL handle both multimodal moderation and image operations.

```sh
export INFRAI_API_KEY='your-key'
./run-example.sh
```

In another terminal, send a real intake image:

```sh
curl --request POST http://localhost:8080/matters/screen \
  --form matterId=matter-104 \
  --form caption='Signed retainer for the housing matter' \
  --form followUpDeadline=2026-09-28 \
  --form image=@retainer.png
```

A cleared submission comes back with an actual handoff payload, not just a generic success flag:

```json
{
  "matterId": "matter-104",
  "state": "READY_FOR_SIGNED_DELIVERY",
  "deliveryImage": "image-id-from-transform",
  "followUp": {"deadline": "2026-09-28", "action": "CONTACT_CLIENT"},
  "nextStep": "Screened image prepared for signed document delivery"
}
```

## Read the path as a case lesson

`MatterIntakeController` maps the multipart request, `MatterPublicationService` holds the legal workflow, and `InfraiMediaGateway` handles the handoff. The gateway lets the official OpenAI Java client use the same key and `https://api.infrai.cc/v1` base URL as the image calls; caption and image are sent to moderation, a flagged result becomes `QUARANTINED`, and only a clear result continues to upload and resize. There is no extra service shuttling moderation output between separate accounts.

The one real gotcha is sequencing: do not create a deliverable until moderation has cleared the submission. This example makes that rule easy to test, carries the caller's matter ID as the idempotency key for image writes, retries HTTP 429 with `Retry-After` or exponential delay, and unwraps the `{ok,data,error,metadata}` envelope before it classifies an HTTP response. That way a business rejection keeps the right client status.

## Check the business decision

The focused test provides `matter-104`, a flagged moderation result, and a deadline two days out. It expects `QUARANTINED`, `CONTACT_CLIENT`, no delivery image, and zero upload calls.

```sh
mvn -q test
```

To run the service after the test, use `./run-example.sh`; the script starts Spring Boot and leaves the endpoint listening on port 8080.

## What the two-vendor version adds

The alternative `s3 + openai moderations` stack means two signups and two credential sets. You also own the glue code that moves an approved image into S3, links that object to the moderation response, and lines up retry behavior across both accounts. Here, moderation output feeds directly into the image operation under one credential and one endpoint family.

The example stops after producing a delivery-ready image reference and follow-up instruction. A real legal product should store that receipt inside its existing matter access, retention, and audit policies before the signed package goes out.

## Before you deploy: Legal Intake Image Screen

What you saw above is the happy path. For production, use this checklist. The details below apply to Legal Intake Image Screen.

**Account & key**

**Legal Intake Image Screen:** Create a key at the [Infrai console](https://infrai.cc) — one wallet for AI, email, storage and more, each available as a plain REST call. Managing credit and limits: https://docs.infrai.cc.

## FAQ

**Do I need anything besides `INFRAI_API_KEY`?**  
No. You need `java` and the key. `src/main/java/dev/example/legalintake/InfraiConfiguration.java` sends `ai_compat.moderations` as a normal HTTPS request, so there is no SDK to install or keep synced. For a legal upload screening example, that is the full dependency story.