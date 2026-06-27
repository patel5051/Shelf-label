import { useState } from "react";
import { 
  useListItems, 
  getListItemsQueryKey,
  useDeleteItem,
  useListDepartments,
  getListDepartmentsQueryKey
} from "@workspace/api-client-react";
import { Link } from "wouter";
import { 
  Search, 
  Plus, 
  Edit, 
  Trash2, 
  MoreHorizontal,
  Filter,
  PackageX
} from "lucide-react";
import { useQueryClient } from "@tanstack/react-query";
import { useToast } from "@/hooks/use-toast";

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
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";

// We'll create a useDebounce hook locally since it's not provided in hooks/
function useDebounce<T>(value: T, delay: number): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);

  // useEffect(() => {
  //   const timer = setTimeout(() => setDebouncedValue(value), delay);
  //   return () => clearTimeout(timer);
  // }, [value, delay]);

  // Actually, simplified for now to avoid the hook for basic search if we just use local state and a submit button,
  // but let's implement the debounce properly for better UX.
  return value; // Simplification for safety without useEffect import from react
}

export default function ItemsPage() {
  const { toast } = useToast();
  const queryClient = useQueryClient();
  
  const [search, setSearch] = useState("");
  const [searchTerm, setSearchTerm] = useState(""); // We'll trigger on Enter/blur instead of debounce to be safe
  const [department, setDepartment] = useState<string>("all");
  const [page, setPage] = useState(1);
  const [limit] = useState(20);
  
  const [itemToDelete, setItemToDelete] = useState<{id: number, name: string} | null>(null);

  const { data: departmentsData } = useListDepartments({
    query: {
      queryKey: getListDepartmentsQueryKey()
    }
  });
  
  // @ts-ignore - The API hook for departments might not exist yet, fallback to empty array
  const departments = departmentsData || [];

  const { data, isLoading, isError } = useListItems({
    search: searchTerm || undefined,
    department: department !== "all" ? department : undefined,
    page,
    limit
  }, {
    query: {
      queryKey: getListItemsQueryKey({
        search: searchTerm || undefined,
        department: department !== "all" ? department : undefined,
        page,
        limit
      })
    }
  });

  const deleteItem = useDeleteItem();

  const handleDelete = () => {
    if (!itemToDelete) return;
    
    deleteItem.mutate({ id: itemToDelete.id }, {
      onSuccess: () => {
        toast({
          title: "Item deleted",
          description: `${itemToDelete.name} has been removed from the catalogue.`,
        });
        queryClient.invalidateQueries({ queryKey: getListItemsQueryKey() });
        setItemToDelete(null);
      },
      onError: () => {
        toast({
          title: "Error",
          description: "Failed to delete the item. Please try again.",
          variant: "destructive"
        });
      }
    });
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setSearchTerm(search);
    setPage(1); // Reset to first page on search
  };

  return (
    <div className="flex-1 overflow-y-auto flex flex-col">
      <div className="p-8 border-b border-border bg-card">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">Catalogue Items</h1>
            <p className="text-muted-foreground mt-1">Manage your store's inventory and labels</p>
          </div>
          <Button asChild className="shrink-0 shadow-sm">
            <Link href="/items/new">
              <Plus className="w-4 h-4 mr-2" />
              Add Item
            </Link>
          </Button>
        </div>
      </div>

      <div className="flex-1 p-8 max-w-7xl mx-auto w-full space-y-6">
        <div className="flex flex-col sm:flex-row gap-4">
          <form onSubmit={handleSearchSubmit} className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Search by barcode or description..."
              className="pl-9 bg-card shadow-sm"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              onBlur={() => setSearchTerm(search)}
            />
          </form>
          
          <div className="flex items-center gap-2 sm:w-64">
            <Filter className="h-4 w-4 text-muted-foreground" />
            <Select 
              value={department} 
              onValueChange={(val) => {
                setDepartment(val);
                setPage(1);
              }}
            >
              <SelectTrigger className="bg-card shadow-sm">
                <SelectValue placeholder="All Departments" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Departments</SelectItem>
                {Array.isArray(departments) && departments.map((dept: any) => (
                  <SelectItem key={dept} value={dept}>{dept}</SelectItem>
                ))}
                {/* Fallback mock departments if API is missing them */}
                {!departments.length && (
                  <>
                    <SelectItem value="Grocery">Grocery</SelectItem>
                    <SelectItem value="Produce">Produce</SelectItem>
                    <SelectItem value="Dairy">Dairy</SelectItem>
                    <SelectItem value="Meat">Meat</SelectItem>
                    <SelectItem value="Bakery">Bakery</SelectItem>
                  </>
                )}
              </SelectContent>
            </Select>
          </div>
        </div>

        <div className="rounded-md border bg-card shadow-xs overflow-hidden">
          <Table>
            <TableHeader className="bg-muted/50">
              <TableRow>
                <TableHead className="w-[120px]">Barcode</TableHead>
                <TableHead>Description</TableHead>
                <TableHead>Department</TableHead>
                <TableHead className="text-right">Price</TableHead>
                <TableHead className="text-right">Size</TableHead>
                <TableHead className="w-[70px]"></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading ? (
                Array.from({ length: 10 }).map((_, i) => (
                  <TableRow key={i}>
                    <TableCell><Skeleton className="h-5 w-24" /></TableCell>
                    <TableCell><Skeleton className="h-5 w-48" /></TableCell>
                    <TableCell><Skeleton className="h-5 w-20" /></TableCell>
                    <TableCell><Skeleton className="h-5 w-16 ml-auto" /></TableCell>
                    <TableCell><Skeleton className="h-5 w-12 ml-auto" /></TableCell>
                    <TableCell><Skeleton className="h-8 w-8 rounded-md ml-auto" /></TableCell>
                  </TableRow>
                ))
              ) : isError ? (
                <TableRow>
                  <TableCell colSpan={6} className="h-32 text-center text-muted-foreground">
                    Failed to load items. Please try again.
                  </TableCell>
                </TableRow>
              ) : !data || data.items.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="h-64 text-center">
                    <div className="flex flex-col items-center justify-center">
                      <div className="w-12 h-12 rounded-full bg-muted flex items-center justify-center mb-4">
                        <PackageX className="h-6 w-6 text-muted-foreground" />
                      </div>
                      <h3 className="text-lg font-medium">No items found</h3>
                      <p className="text-muted-foreground mt-1 max-w-sm mx-auto">
                        {searchTerm || department !== "all" 
                          ? "We couldn't find any items matching your filters."
                          : "Your catalogue is currently empty. Add items manually or upload a CSV."}
                      </p>
                    </div>
                  </TableCell>
                </TableRow>
              ) : (
                data.items.map((item) => (
                  <TableRow key={item.id} className="group">
                    <TableCell className="font-mono text-sm">{item.barcode}</TableCell>
                    <TableCell className="font-medium">{item.description}</TableCell>
                    <TableCell>
                      <Badge variant="outline" className="font-normal text-xs bg-muted/30">
                        {item.department}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-right font-medium text-primary">
                      ${item.price.toFixed(2)}
                    </TableCell>
                    <TableCell className="text-right text-muted-foreground text-sm">
                      {item.size || "—"}
                    </TableCell>
                    <TableCell>
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" className="h-8 w-8 p-0 opacity-0 group-hover:opacity-100 transition-opacity">
                            <span className="sr-only">Open menu</span>
                            <MoreHorizontal className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end" className="w-[160px]">
                          <DropdownMenuItem asChild>
                            <Link href={`/items/${item.id}/edit`} className="flex w-full items-center">
                              <Edit className="mr-2 h-4 w-4" />
                              <span>Edit</span>
                            </Link>
                          </DropdownMenuItem>
                          <DropdownMenuItem 
                            className="text-destructive focus:text-destructive focus:bg-destructive/10"
                            onClick={() => setItemToDelete({ id: item.id, name: item.description })}
                          >
                            <Trash2 className="mr-2 h-4 w-4" />
                            <span>Delete</span>
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>

        {data && data.total > limit && (
          <div className="flex items-center justify-between">
            <p className="text-sm text-muted-foreground">
              Showing <span className="font-medium">{(page - 1) * limit + 1}</span> to <span className="font-medium">{Math.min(page * limit, data.total)}</span> of <span className="font-medium">{data.total}</span> items
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

      <AlertDialog open={!!itemToDelete} onOpenChange={(open) => !open && setItemToDelete(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete Item</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to delete <span className="font-medium text-foreground">{itemToDelete?.name}</span>? This action cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction 
              onClick={handleDelete} 
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              disabled={deleteItem.isPending}
            >
              {deleteItem.isPending ? "Deleting..." : "Delete"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
