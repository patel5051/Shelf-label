import { 
  useGetDashboardStats, 
  getGetDashboardStatsQueryKey 
} from "@workspace/api-client-react";
import { 
  Package, 
  Tags, 
  Printer, 
  History, 
  ArrowRight,
  TrendingUp
} from "lucide-react";
import { Link } from "wouter";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { 
  BarChart, 
  Bar, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  ResponsiveContainer,
  Cell
} from "recharts";

export default function DashboardPage() {
  const { data: stats, isLoading, isError } = useGetDashboardStats({
    query: {
      queryKey: getGetDashboardStatsQueryKey(),
    }
  });

  return (
    <div className="flex-1 overflow-y-auto p-8 bg-muted/30">
      <div className="max-w-6xl mx-auto space-y-8">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold tracking-tight text-foreground">Overview</h1>
            <p className="text-muted-foreground mt-1">Catalogue stats and printing activity</p>
          </div>
          <div className="flex items-center gap-3">
            <Link href="/upload" className="inline-flex items-center justify-center whitespace-nowrap rounded-md text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50 border border-input bg-background shadow-xs hover:bg-accent hover:text-accent-foreground h-9 px-4 py-2">
              Import CSV
            </Link>
            <Link href="/items/new" className="inline-flex items-center justify-center whitespace-nowrap rounded-md text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50 bg-primary text-primary-foreground shadow-xs hover:bg-primary/90 h-9 px-4 py-2">
              Add Item
            </Link>
          </div>
        </div>

        {isLoading ? (
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
            {Array.from({ length: 4 }).map((_, i) => (
              <Card key={i}>
                <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
                  <Skeleton className="h-4 w-24" />
                  <Skeleton className="h-4 w-4 rounded-full" />
                </CardHeader>
                <CardContent>
                  <Skeleton className="h-8 w-16 mb-1" />
                  <Skeleton className="h-3 w-32" />
                </CardContent>
              </Card>
            ))}
          </div>
        ) : isError || !stats ? (
          <div className="p-12 text-center rounded-lg border border-dashed border-border bg-background">
            <h3 className="text-lg font-medium">Failed to load dashboard data</h3>
            <p className="text-sm text-muted-foreground mt-2">Check your connection and try again.</p>
          </div>
        ) : (
          <>
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
              <Card>
                <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
                  <CardTitle className="text-sm font-medium text-muted-foreground">Total Items</CardTitle>
                  <Package className="h-4 w-4 text-primary" />
                </CardHeader>
                <CardContent>
                  <div className="text-3xl font-bold">{stats.totalItems.toLocaleString()}</div>
                  <p className="text-xs text-muted-foreground mt-1">In catalogue</p>
                </CardContent>
              </Card>
              <Card>
                <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
                  <CardTitle className="text-sm font-medium text-muted-foreground">Departments</CardTitle>
                  <Tags className="h-4 w-4 text-primary" />
                </CardHeader>
                <CardContent>
                  <div className="text-3xl font-bold">{stats.totalDepartments.toLocaleString()}</div>
                  <p className="text-xs text-muted-foreground mt-1">Active categories</p>
                </CardContent>
              </Card>
              <Card>
                <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
                  <CardTitle className="text-sm font-medium text-muted-foreground">Printed Today</CardTitle>
                  <Printer className="h-4 w-4 text-primary" />
                </CardHeader>
                <CardContent>
                  <div className="text-3xl font-bold">{stats.printedToday.toLocaleString()}</div>
                  <p className="text-xs text-muted-foreground mt-1">Labels generated</p>
                </CardContent>
              </Card>
              <Card>
                <CardHeader className="flex flex-row items-center justify-between pb-2 space-y-0">
                  <CardTitle className="text-sm font-medium text-muted-foreground">Printed This Week</CardTitle>
                  <TrendingUp className="h-4 w-4 text-primary" />
                </CardHeader>
                <CardContent>
                  <div className="text-3xl font-bold">{stats.printedThisWeek.toLocaleString()}</div>
                  <p className="text-xs text-muted-foreground mt-1">Labels generated</p>
                </CardContent>
              </Card>
            </div>

            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-7">
              <Card className="col-span-4">
                <CardHeader>
                  <CardTitle>Items by Department</CardTitle>
                  <CardDescription>Distribution of items across store departments</CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="h-[300px] w-full">
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart
                        data={stats.itemsByDepartment}
                        margin={{ top: 20, right: 30, left: 0, bottom: 25 }}
                      >
                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="hsl(var(--border))" />
                        <XAxis 
                          dataKey="department" 
                          axisLine={false}
                          tickLine={false}
                          tick={{ fill: 'hsl(var(--muted-foreground))', fontSize: 12 }}
                          dy={10}
                          angle={-45}
                          textAnchor="end"
                        />
                        <YAxis 
                          axisLine={false}
                          tickLine={false}
                          tick={{ fill: 'hsl(var(--muted-foreground))', fontSize: 12 }}
                          dx={-10}
                        />
                        <Tooltip
                          cursor={{ fill: 'hsl(var(--muted))' }}
                          contentStyle={{ 
                            backgroundColor: 'hsl(var(--card))', 
                            borderRadius: 'var(--radius)',
                            border: '1px solid hsl(var(--border))',
                            boxShadow: 'var(--shadow-sm)'
                          }}
                        />
                        <Bar 
                          dataKey="count" 
                          radius={[4, 4, 0, 0]}
                          maxBarSize={50}
                        >
                          {stats.itemsByDepartment.map((entry, index) => (
                            <Cell key={`cell-${index}`} fill="hsl(var(--primary))" />
                          ))}
                        </Bar>
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                </CardContent>
              </Card>

              <Card className="col-span-3">
                <CardHeader className="flex flex-row items-center justify-between">
                  <div className="space-y-1">
                    <CardTitle>Recent Prints</CardTitle>
                    <CardDescription>Latest generated labels</CardDescription>
                  </div>
                  <Button variant="ghost" size="sm" asChild>
                    <Link href="/print-history" className="text-xs">
                      View All
                    </Link>
                  </Button>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    {stats.recentPrints.length === 0 ? (
                      <div className="text-center py-8 text-muted-foreground text-sm">
                        No print history yet
                      </div>
                    ) : (
                      stats.recentPrints.slice(0, 5).map((print) => (
                        <div key={print.id} className="flex items-center justify-between">
                          <div className="flex items-center gap-3">
                            <div className="w-9 h-9 rounded-full bg-primary/10 flex items-center justify-center">
                              <History className="w-4 h-4 text-primary" />
                            </div>
                            <div className="space-y-0.5">
                              <p className="text-sm font-medium leading-none truncate max-w-[150px]" title={print.description}>
                                {print.description}
                              </p>
                              <p className="text-xs text-muted-foreground">
                                {print.barcode} • {new Date(print.printedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                              </p>
                            </div>
                          </div>
                          <div className="font-mono text-sm font-medium">
                            ${print.price.toFixed(2)}
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                </CardContent>
              </Card>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
