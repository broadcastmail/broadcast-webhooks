# broadcastmail-webhooks

Webhook receiver for BroadcastMail. Handles incoming events from Stripe and Resend and updates campaign and account state accordingly.

## Responsibilities

- Stripe — `checkout.session.completed`, `customer.subscription.updated`, `customer.subscription.deleted`
- Resend — delivery events: delivered, opened, bounced, complained

See [ARCHITECTURE.md](https://github.com/broadcastmail/.github/blob/main/ARCHITECTURE.md) for how this service fits into the full system.

## Getting started

See [DEVELOPERS.md](DEVELOPERS.md) for local setup.

## Contributing

See [CONTRIBUTING.md](https://github.com/broadcastmail/.github/blob/main/CONTRIBUTING.md).

## License

MIT
