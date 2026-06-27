import { useLocation, useParams } from "wouter";
import { useEffect, useRef } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { 
  useGetItem, 
  getGetItemQueryKey,
  useUpdateItem 
} from "@workspace/api-client-react";
import { useToast } from "@/hooks/use-toast";
import { useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Save } from "lucide-react";
import { Link } from "wouter";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";

const itemSchema = z.object({
  barcode: z.string().min(1, "Barcode is required"),
  description: z.string().min(1, "Description is required"),
  price: z.coerce.number().min(0, "Price must be positive"),
  department: z.string().min(1, "Department is required"),
  size: z.string().optional().default(""),
});

type ItemFormValues = z.infer<typeof itemSchema>;

export default function EditItemPage() {
  const [, setLocation] = useLocation();
  const { id } = useParams<{ id: string }>();
  const itemId = parseInt(id || "0", 10);
  const { toast } = useToast();
  const queryClient = useQueryClient();
  
  const form = useForm<ItemFormValues>({
    resolver: zodResolver(itemSchema),
    defaultValues: {
      barcode: "",
      description: "",
      price: 0,
      department: "",
      size: "",
    },
  });

  const { data: item, isLoading, isError } = useGetItem(itemId, {
    query: {
      enabled: !!itemId && !isNaN(itemId),
      queryKey: getGetItemQueryKey(itemId),
    }
  });

  // Track initialization to avoid resetting user edits if a refetch happens
  const initialized = useRef(false);

  useEffect(() => {
    if (item && !initialized.current) {
      form.reset({
        barcode: item.barcode,
        description: item.description,
        price: item.price,
        department: item.department,
        size: item.size || "",
      });
      initialized.current = true;
    }
  }, [item, form]);

  const updateItem = useUpdateItem();

  const onSubmit = (data: ItemFormValues) => {
    updateItem.mutate({ id: itemId, data }, {
      onSuccess: () => {
        toast({
          title: "Item updated",
          description: "Changes have been saved successfully.",
        });
        queryClient.invalidateQueries({ queryKey: getGetItemQueryKey(itemId) });
        queryClient.invalidateQueries({ queryKey: ['/api/items'] });
        setLocation("/items");
      },
      onError: (err) => {
        const msg = (err.data as { error?: string } | undefined)?.error;
        toast({
          title: "Error updating item",
          description: msg || "Failed to update item. Please check your data.",
          variant: "destructive",
        });
      }
    });
  };

  if (isError) {
    return (
      <div className="flex-1 p-8 bg-muted/30">
        <div className="max-w-3xl mx-auto text-center p-12 bg-card rounded-lg border border-dashed">
          <h2 className="text-xl font-bold">Item not found</h2>
          <p className="text-muted-foreground mt-2 mb-6">The item you're trying to edit doesn't exist or there was an error loading it.</p>
          <Button asChild>
            <Link href="/items">Back to Catalogue</Link>
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-y-auto bg-muted/30">
      <div className="max-w-3xl mx-auto p-8 space-y-6">
        <div className="flex items-center gap-4">
          <Button variant="outline" size="icon" asChild className="h-9 w-9 bg-card shadow-sm">
            <Link href="/items">
              <ArrowLeft className="h-4 w-4" />
            </Link>
          </Button>
          <div>
            <h1 className="text-2xl font-bold tracking-tight">Edit Item</h1>
            <p className="text-muted-foreground text-sm">Update product information</p>
          </div>
        </div>

        <Card className="shadow-sm">
          <CardHeader>
            <CardTitle>Item Details</CardTitle>
            <CardDescription>Make changes to the product info used for shelf labels.</CardDescription>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="space-y-6">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div className="space-y-2">
                    <Skeleton className="h-4 w-20" />
                    <Skeleton className="h-10 w-full" />
                  </div>
                  <div className="space-y-2">
                    <Skeleton className="h-4 w-24" />
                    <Skeleton className="h-10 w-full" />
                  </div>
                  <div className="space-y-2 md:col-span-2">
                    <Skeleton className="h-4 w-24" />
                    <Skeleton className="h-10 w-full" />
                  </div>
                  <div className="space-y-2">
                    <Skeleton className="h-4 w-16" />
                    <Skeleton className="h-10 w-full" />
                  </div>
                  <div className="space-y-2">
                    <Skeleton className="h-4 w-28" />
                    <Skeleton className="h-10 w-full" />
                  </div>
                </div>
                <div className="flex justify-end gap-3 pt-6 border-t border-border mt-8">
                  <Skeleton className="h-10 w-20" />
                  <Skeleton className="h-10 w-32" />
                </div>
              </div>
            ) : (
              <Form {...form}>
                <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <FormField
                      control={form.control}
                      name="barcode"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Barcode / SKU</FormLabel>
                          <FormControl>
                            <Input placeholder="e.g. 123456789012" {...field} className="font-mono text-sm bg-muted/50" />
                          </FormControl>
                          <FormDescription>The unique identifier for scanning.</FormDescription>
                          <FormMessage />
                        </FormItem>
                      )}
                    />

                    <FormField
                      control={form.control}
                      name="department"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Department</FormLabel>
                          <FormControl>
                            <Input placeholder="e.g. Grocery" {...field} />
                          </FormControl>
                          <FormDescription>Store section where this item lives.</FormDescription>
                          <FormMessage />
                        </FormItem>
                      )}
                    />

                    <FormField
                      control={form.control}
                      name="description"
                      render={({ field }) => (
                        <FormItem className="md:col-span-2">
                          <FormLabel>Description</FormLabel>
                          <FormControl>
                            <Input placeholder="e.g. Organic Bananas" {...field} />
                          </FormControl>
                          <FormDescription>The main product name shown on the label.</FormDescription>
                          <FormMessage />
                        </FormItem>
                      )}
                    />

                    <FormField
                      control={form.control}
                      name="price"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Price ($)</FormLabel>
                          <FormControl>
                            <div className="relative">
                              <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground">$</span>
                              <Input type="number" step="0.01" min="0" placeholder="0.00" className="pl-7 font-mono text-sm" {...field} />
                            </div>
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />

                    <FormField
                      control={form.control}
                      name="size"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Size / Weight (Optional)</FormLabel>
                          <FormControl>
                            <Input placeholder="e.g. 1 lb, 500ml" {...field} />
                          </FormControl>
                          <FormDescription>Unit of measurement.</FormDescription>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>

                  <div className="flex justify-end gap-3 pt-6 border-t border-border mt-8">
                    <Button variant="outline" type="button" asChild>
                      <Link href="/items">Cancel</Link>
                    </Button>
                    <Button type="submit" disabled={updateItem.isPending} className="shadow-xs">
                      {updateItem.isPending ? (
                        "Saving..."
                      ) : (
                        <>
                          <Save className="w-4 h-4 mr-2" />
                          Save Changes
                        </>
                      )}
                    </Button>
                  </div>
                </form>
              </Form>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
