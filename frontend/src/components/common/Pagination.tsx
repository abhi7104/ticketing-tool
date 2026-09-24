import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";

interface Props {
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

export function Pagination({ page, size, totalItems, totalPages, onPageChange }: Props) {
  if (totalItems === 0) return null;
  const from = page * size + 1;
  const to = Math.min(totalItems, (page + 1) * size);
  return (
    <nav aria-label="Pagination" className="flex items-center justify-between gap-4 px-4 py-3 text-sm">
      <p className="text-muted-foreground" aria-live="polite">
        Showing <span className="font-medium text-foreground">{from}–{to}</span> of{" "}
        <span className="font-medium text-foreground">{totalItems}</span>
      </p>
      <div className="flex gap-2">
        <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => onPageChange(page - 1)}>
          <ChevronLeft aria-hidden />
          Previous
        </Button>
        <Button
          variant="secondary"
          size="sm"
          disabled={page + 1 >= totalPages}
          onClick={() => onPageChange(page + 1)}
        >
          Next
          <ChevronRight aria-hidden />
        </Button>
      </div>
    </nav>
  );
}
