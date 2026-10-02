# Taxonomy and sample captures (validation)

Starter categories (onboarding defaults — user can rename/add):

1. Gym and fitness
2. Cooking and recipes
3. Funny moments
4. Work and career
5. Travel ideas
6. Money and finance tips
7. Health and wellness
8. Learning and books
9. Shopping and products
10. Relationships and social

## Sample capture → expected items

| Raw capture (abbreviated) | Extracted items | Suggested categories |
| ------------------------- | --------------- | -------------------- |
| "Leg day: 3x10 RDL, rest 90s" | Single tip | Gym and fitness |
| Chat paste with recipe + joke | 2 items | Cooking; Funny moments |
| "ok" / sticker-only | `no_items` | Raw capture kept searchable |
| Reel link about air fryer | 1 item + URL metadata | Cooking |
| Screenshot of DM thread | OCR text → 1–N items | Per content |

## Review UX rules

- **Auto-file** when model confidence ≥ 0.85 and category already exists.
- **Review queue** when new category proposed, split ambiguous, or confidence < 0.85.
- Bulk accept/reject on review inbox.
