import { z } from "zod";

export const captureStatusSchema = z.enum(["QUEUED", "PROCESSING", "READY", "FAILED", "NO_ITEMS"]);
export const itemStatusSchema = z.enum(["PENDING_REVIEW", "FILED", "ARCHIVED"]);

export const createCaptureSchema = z.object({
  source: z.string().min(1).max(64),
  rawText: z.string().max(50_000).optional(),
  sourceUrl: z.string().url().optional().or(z.literal("")),
  conversationTitle: z.string().max(255).optional(),
  clientCaptureId: z.string().max(64).optional(),
});

export type CreateCaptureInput = z.infer<typeof createCaptureSchema>;

export const captureSchema = z.object({
  id: z.string().uuid(),
  source: z.string(),
  rawText: z.string().optional(),
  status: captureStatusSchema,
  errorMessage: z.string().optional(),
  createdAt: z.string(),
  items: z.array(
    z.object({
      id: z.string().uuid(),
      title: z.string(),
      status: itemStatusSchema,
    }),
  ),
});

export type CaptureDto = z.infer<typeof captureSchema>;
