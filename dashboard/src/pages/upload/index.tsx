import { useState, useRef } from "react";
import { useBulkUpsertItems, type ItemInput } from "@workspace/api-client-react";
import { useToast } from "@/hooks/use-toast";
import { 
  Upload as UploadIcon, 
  FileSpreadsheet, 
  CheckCircle2, 
  AlertCircle,
  FileWarning,
  RefreshCw,
  X
} from "lucide-react";
import { parseCSV } from "@/lib/csv";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle, CardFooter } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Progress } from "@/components/ui/progress";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";

export default function UploadPage() {
  const { toast } = useToast();
  const fileInputRef = useRef<HTMLInputElement>(null);
  
  const [file, setFile] = useState<File | null>(null);
  const [parsedRows, setParsedRows] = useState<ItemInput[]>([]);
  const [errors, setErrors] = useState<string[]>([]);
  const [isProcessing, setIsProcessing] = useState(false);
  const [result, setResult] = useState<{inserted: number, updated: number, failed: number} | null>(null);

  const bulkUpsert = useBulkUpsertItems();

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0];
    if (!selectedFile) return;

    if (!selectedFile.name.endsWith('.csv')) {
      toast({
        title: "Invalid file type",
        description: "Please select a .csv file",
        variant: "destructive"
      });
      return;
    }

    setFile(selectedFile);
    setResult(null);
    processCSV(selectedFile);
  };

  const processCSV = (file: File) => {
    setIsProcessing(true);
    const reader = new FileReader();
    
    reader.onload = (e) => {
      try {
        const text = e.target?.result as string;
        const rows = parseCSV(text);
        
        const validItems: ItemInput[] = [];
        const foundErrors: string[] = [];
        
        rows.forEach((row, index) => {
          // Map standard CSV headers to our schema if needed, or assume exact match
          const barcode = row.barcode || row.sku || "";
          const description = row.description || row.name || row.item || "";
          const priceRaw = row.price || row.cost || "0";
          const price = parseFloat(priceRaw.replace(/[^0-9.]/g, ''));
          const department = row.department || row.category || "General";
          const size = row.size || row.weight || "";
          
          if (!barcode) {
            foundErrors.push(`Row ${index + 2}: Missing barcode`);
            return;
          }
          if (!description) {
            foundErrors.push(`Row ${index + 2}: Missing description`);
            return;
          }
          if (isNaN(price)) {
            foundErrors.push(`Row ${index + 2}: Invalid price format '${priceRaw}'`);
            return;
          }
          
          validItems.push({
            barcode,
            description,
            price,
            department,
            size
          });
        });
        
        setParsedRows(validItems);
        setErrors(foundErrors);
      } catch (err) {
        console.error("CSV Parse Error:", err);
        setErrors(["Failed to parse the CSV file. Please check its formatting."]);
      } finally {
        setIsProcessing(false);
      }
    };
    
    reader.onerror = () => {
      setErrors(["Error reading the file"]);
      setIsProcessing(false);
    };
    
    reader.readAsText(file);
  };

  const handleUpload = () => {
    if (parsedRows.length === 0) return;
    
    bulkUpsert.mutate({ data: { items: parsedRows } }, {
      onSuccess: (data) => {
        setResult(data);
        toast({
          title: "Upload complete",
          description: `Successfully processed ${parsedRows.length} items.`,
        });
      },
      onError: (err) => {
        const msg = (err.data as { error?: string } | undefined)?.error;
        toast({
          title: "Upload failed",
          description: msg || "There was a server error processing your upload.",
          variant: "destructive"
        });
      }
    });
  };

  const resetState = () => {
    setFile(null);
    setParsedRows([]);
    setErrors([]);
    setResult(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  return (
    <div className="flex-1 overflow-y-auto bg-muted/30 p-8">
      <div className="max-w-4xl mx-auto space-y-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-foreground">Bulk Upload</h1>
          <p className="text-muted-foreground mt-1">Import items into your catalogue via CSV</p>
        </div>

        {!file ? (
          <Card className="border-dashed border-2 border-border shadow-none bg-card/50">
            <CardContent className="flex flex-col items-center justify-center p-12 text-center">
              <div className="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center mb-6">
                <FileSpreadsheet className="w-8 h-8 text-primary" />
              </div>
              <h3 className="text-lg font-semibold mb-2">Select a CSV file to upload</h3>
              <p className="text-muted-foreground text-sm max-w-sm mb-8">
                Your file should contain columns for barcode, description, price, department, and size.
              </p>
              
              <input 
                type="file" 
                accept=".csv" 
                className="hidden" 
                ref={fileInputRef}
                onChange={handleFileChange}
              />
              
              <Button onClick={() => fileInputRef.current?.click()} className="shadow-sm">
                <UploadIcon className="w-4 h-4 mr-2" />
                Browse Files
              </Button>
            </CardContent>
          </Card>
        ) : (
          <div className="space-y-6">
            <Card>
              <CardHeader className="pb-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <FileSpreadsheet className="w-8 h-8 text-primary" />
                    <div>
                      <CardTitle className="text-base">{file.name}</CardTitle>
                      <CardDescription>
                        {isProcessing ? "Parsing file..." : `${parsedRows.length} valid rows found`}
                      </CardDescription>
                    </div>
                  </div>
                  {!result && (
                    <Button variant="ghost" size="icon" onClick={resetState}>
                      <X className="w-4 h-4" />
                    </Button>
                  )}
                </div>
              </CardHeader>

              {result ? (
                <CardContent>
                  <Alert className="bg-emerald-50 text-emerald-900 border-emerald-200 dark:bg-emerald-900/20 dark:text-emerald-200 dark:border-emerald-800 mb-6">
                    <CheckCircle2 className="h-4 w-4 !text-emerald-600 dark:!text-emerald-400" />
                    <AlertTitle>Upload Successful</AlertTitle>
                    <AlertDescription>
                      The catalogue has been updated with the contents of your CSV.
                    </AlertDescription>
                  </Alert>

                  <div className="grid grid-cols-3 gap-4 mb-6">
                    <div className="bg-card border rounded-lg p-4 text-center">
                      <div className="text-3xl font-bold text-foreground">{result.inserted}</div>
                      <div className="text-xs text-muted-foreground uppercase tracking-wider font-semibold mt-1">New Items</div>
                    </div>
                    <div className="bg-card border rounded-lg p-4 text-center">
                      <div className="text-3xl font-bold text-foreground">{result.updated}</div>
                      <div className="text-xs text-muted-foreground uppercase tracking-wider font-semibold mt-1">Updated</div>
                    </div>
                    <div className="bg-card border rounded-lg p-4 text-center">
                      <div className="text-3xl font-bold text-destructive">{result.failed}</div>
                      <div className="text-xs text-muted-foreground uppercase tracking-wider font-semibold mt-1">Failed</div>
                    </div>
                  </div>

                  <div className="flex justify-end">
                    <Button onClick={resetState}>Upload Another File</Button>
                  </div>
                </CardContent>
              ) : (
                <>
                  <CardContent className="p-0 border-t border-border">
                    {errors.length > 0 && (
                      <div className="p-4 bg-amber-50 border-b border-amber-200 dark:bg-amber-900/10 dark:border-amber-900/30">
                        <div className="flex gap-2">
                          <FileWarning className="w-5 h-5 text-amber-600 dark:text-amber-500 shrink-0" />
                          <div>
                            <h4 className="font-medium text-amber-900 dark:text-amber-400 text-sm">Found {errors.length} issues in the data</h4>
                            <ul className="mt-2 text-sm text-amber-800 dark:text-amber-500/80 list-disc pl-4 space-y-1">
                              {errors.slice(0, 5).map((err, i) => (
                                <li key={i}>{err}</li>
                              ))}
                              {errors.length > 5 && (
                                <li>...and {errors.length - 5} more issues. These rows will be skipped.</li>
                              )}
                            </ul>
                          </div>
                        </div>
                      </div>
                    )}
                    
                    <ScrollArea className="h-[400px]">
                      <Table>
                        <TableHeader className="sticky top-0 bg-muted/80 backdrop-blur z-10 shadow-sm">
                          <TableRow>
                            <TableHead className="w-[150px]">Barcode</TableHead>
                            <TableHead>Description</TableHead>
                            <TableHead>Department</TableHead>
                            <TableHead className="text-right">Price</TableHead>
                          </TableRow>
                        </TableHeader>
                        <TableBody>
                          {parsedRows.slice(0, 100).map((row, i) => (
                            <TableRow key={i}>
                              <TableCell className="font-mono text-sm">{row.barcode}</TableCell>
                              <TableCell className="font-medium">{row.description}</TableCell>
                              <TableCell className="text-muted-foreground text-sm">{row.department}</TableCell>
                              <TableCell className="text-right font-medium">${row.price.toFixed(2)}</TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                      {parsedRows.length > 100 && (
                        <div className="p-4 text-center text-sm text-muted-foreground border-t">
                          Showing first 100 of {parsedRows.length} valid rows
                        </div>
                      )}
                    </ScrollArea>
                  </CardContent>
                  <CardFooter className="flex justify-between items-center bg-muted/20 border-t p-4">
                    <p className="text-sm text-muted-foreground">
                      Ready to import <span className="font-bold text-foreground">{parsedRows.length}</span> items. Existing barcodes will be updated.
                    </p>
                    <div className="flex gap-3">
                      <Button variant="outline" onClick={resetState} disabled={bulkUpsert.isPending}>
                        Cancel
                      </Button>
                      <Button 
                        onClick={handleUpload} 
                        disabled={parsedRows.length === 0 || bulkUpsert.isPending}
                        className="shadow-xs"
                      >
                        {bulkUpsert.isPending ? (
                          <>
                            <RefreshCw className="w-4 h-4 mr-2 animate-spin" />
                            Importing...
                          </>
                        ) : (
                          <>
                            <UploadIcon className="w-4 h-4 mr-2" />
                            Start Import
                          </>
                        )}
                      </Button>
                    </div>
                  </CardFooter>
                </>
              )}
            </Card>
          </div>
        )}
      </div>
    </div>
  );
}
