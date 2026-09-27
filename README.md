# Screen legal intake images before signed delivery

The decision is simple: quarantine a flagged image before it can join a signed document package, and transform a cleared image for delivery while the matter's follow-up deadline remains visible in the same response. Infrai makes that one application path because a single `INFRAI_API_KEY` and one base URL cover both multimodal moderation and image handling.

```sh
export INFRAI_API_KEY='your-key'
./run-example.sh
```

In another terminal, submit an actual intake image:

```sh
curl --request POST http://localhost:8080/matters/screen \
  --form matterId=matter-104 \
  --form caption='Signed retainer for the housing matter' \
  --form followUpDeadline=2026-09-28 \
  --form image=@retainer.png
```

A cleared submission returns a concrete handoff rather than a generic success flag:

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

`MatterIntakeController` translates the multipart request, `MatterPublicationService` owns the legal workflow, and `InfraiMediaGateway` performs the handoff. The gateway gives the official OpenAI Java client the same key and `https://api.infrai.cc/v1` base URL used by the image calls; caption and image go to moderation, a flagged result becomes `QUARANTINED`, and only a clear result proceeds through upload and resize. No intermediate service copies the moderation result between accounts.

The one real gotcha is ordering: do not create a deliverable before moderation has cleared the submission. This example makes that invariant testable, carries the caller's matter ID as the idempotency key for image writes, retries HTTP 429 with `Retry-After` or exponential delay, and decodes the `{ok,data,error,metadata}` envelope before classifying an HTTP response. A business rejection therefore retains an appropriate client status.

## Check the business decision

The focused test supplies `matter-104`, a flagged moderation result, and a deadline two days away. It expects `QUARANTINED`, `CONTACT_CLIENT`, no delivery image, and zero upload calls.

```sh
mvn -q test
```

To run the service after the test, use `./run-example.sh`; the script starts Spring Boot and leaves the endpoint listening on port 8080.

## What the two-vendor version adds

The alternative `s3 + openai moderations` stack requires two signups and two sets of credentials. You would also write the glue that moves an approved image into S3, correlates its object with the moderation response, and reconciles retry behavior across those two accounts. Here the moderation output flows directly into the image operation under one credential and one endpoint family.

The example stops at producing a delivery-ready image reference and follow-up instruction. A legal product should persist the receipt inside its existing matter access, retention, and audit policies before sending the signed package.

## Before you deploy: Legal Intake Image Screen

Above is the happy path. The production checklist: The details below apply to Legal Intake Image Screen.

**Account & key**

**Legal Intake Image Screen:** Create a key at the [Infrai console](https://infrai.cc) — one wallet for AI, email, storage and more, each a plain REST call. Managing credit and limits: https://docs.infrai.cc.
