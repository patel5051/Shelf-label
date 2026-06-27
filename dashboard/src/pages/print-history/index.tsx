import { useState } from "react";
import { 
  useListPrintHistory, 
  getListPrintHistoryQueryKey
} from "@workspace/api-client-react";
import { 
  Search, 
  Printer, 
  Calendar,
  History
} from "lucide-react";
import { format } from "date-fns";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";

export default function PrintHistoryPage() {
  const [search, setSearch] = useState("");
  const [searchTerm, setSearchTerm] = useState(""); 
  const [page, setPage] = useState(1);
  const [limit] = useState(20);

  const { data, isLoading, isError } = useListPrintHistory({
    search: searchTerm || undefined,
    page,
    limit
  }, {
    query: {
      queryKey: getListPrintHistoryQueryKey({
        search: searchTerm || undefined,
        page,
        limit
      })
    }
  });

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setSearchTerm(search);
    setPage(1);
  };

  return (
    <div className="flex-1 overflow-y-auto flex flex-col">
      <div className="p-8 border-b border-border bg-card">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">Print History</h1>
            <p className="text-muted-foreground mt-1">Audit log of all shelf labels printed</p>
          </div>
        </div>
      </div>

      <div className="flex-1 p-8 max-w-7xl mx-auto w-full space-y-6">
        <div className="flex flex-col sm:flex-row gap-4">
          <form onSubmit={handleSearchSubmit} className="relative flex-1 max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Search history by barcode or description..."
              className="pl-9 bg-card shadow-sm"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              onBlur={() => setSearchTerm(search)}
            />
          </form>
        </div>

        <Card className="overflow-hidden shadow-sm">
          <Table>
            <TableHeader className="bg-muted/50">
              <TableRow>
                <TableHead className="w-[180px]">Printed At</TableHead>
                <TableHead className="w-[120px]">Barcode</TableHead>
                <TableHead>Description</TableHead>
                <TableHead className="text-right">Price at Print</TableHead>
                <TableHead>Status</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading ? (
                Array.from({ length: 10 }).map((_, i) => (
                  <TableRow key={i}>
                    <TableCell><Skeleton className="h-5 w-32" /></TableCell>
                    <TableCell><Skeleton className="h-5 w-24" /></TableCell>
                    <TableCell><Skeleton className="h-5 w-48" /></TableCell>
                    <TableCell><Skeleton className="h-5 w-16 ml-auto" /></TableCell>
                    <TableCell><Skeleton className="h-6 w-20 rounded-full" /></TableCell>
                  </TableRow>
                ))
              ) : isError ? (
                <TableRow>
                  <TableCell colSpan={5} className="h-32 text-center text-muted-foreground">
                    Failed to load history. Please try again.
                  </TableCell>
                </TableRow>
              ) : !data || data.entries.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} className="h-64 text-center">
                    <div className="flex flex-col items-center justify-center">
                      <div className="w-12 h-12 rounded-full bg-muted flex items-center justify-center mb-4">
                        <History className="h-6 w-6 text-muted-foreground" />
                      </div>
                      <h3 className="text-lg font-medium">No prints found</h3>
                      <p className="text-muted-foreground mt-1 max-w-sm mx-auto">
                        {searchTerm
                          ? "We couldn't find any print records matching your search."
                          : "There is no record of any printed labels yet."}
                      </p>
                    </div>
                  </TableCell>
                </TableRow>
              ) : (
                data.entries.map((entry) => (
                  <TableRow key={entry.id}>
                    <TableCell className="text-sm font-medium">
                      <div className="flex items-center gap-2">
                        <Calendar className="w-3.5 h-3.5 text-muted-foreground" />
                        {format(new Date(entry.printedAt), "MMM d, yyyy h:mm a")}
                      </div>
                    </TableCell>
                    <TableCell className="font-mono text-sm text-muted-foreground">{entry.barcode}</TableCell>
                    <TableCell className="font-medium">{entry.description}</TableCell>
                    <TableCell className="text-right font-medium">
                      ${entry.price.toFixed(2)}
                    </TableCell>
                    <TableCell>
                      <Badge variant="outline" className="font-normal text-xs bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-500/10 dark:text-emerald-400 dark:border-emerald-500/20">
                        <Printer className="w-3 h-3 mr-1" />
                        Printed
                      </Badge>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </Card>

        {data && data.total > limit && (
          <div className="flex items-center justify-between">
            <p className="text-sm text-muted-foreground">
              Showing <span className="font-medium">{(page - 1) * limit + 1}</span> to <span className="font-medium">{Math.min(page * limit, data.total)}</span> of <span className="font-medium">{data.total}</span> records
            </p>
            <div className="flex gap-2">
              <Button 
                variant="outline" 
                size="sm" 
                onClick={() => setPage(p => Math.max(1, p - 1))}
                disabled={page === 1}
              >
                Previous
              </Button>
              <Button 
                variant="outline" 
                size="sm"
                onClick={() => setPage(p => p + 1)}
                disabled={page * limit >= data.total}
              >
                Next
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
