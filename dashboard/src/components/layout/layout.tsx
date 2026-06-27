import { Link, useLocation } from "wouter";
import { 
  LayoutDashboard, 
  Package, 
  History, 
  Upload, 
  Plus
} from "lucide-react";
import { cn } from "@/lib/utils";
import { ReactNode } from "react";

const navItems = [
  { href: "/", label: "Dashboard", icon: LayoutDashboard },
  { href: "/items", label: "Catalogue", icon: Package },
  { href: "/print-history", label: "Print History", icon: History },
  { href: "/upload", label: "Bulk Upload", icon: Upload },
];

export function Layout({ children }: { children: ReactNode }) {
  const [location] = useLocation();

  return (
    <div className="flex min-h-screen w-full bg-background">
      <aside className="w-64 flex-shrink-0 bg-sidebar border-r border-sidebar-border flex flex-col">
        <div className="h-16 flex items-center px-6 border-b border-sidebar-border">
          <div className="font-semibold text-lg text-sidebar-foreground tracking-tight flex items-center gap-2">
            <Package className="w-5 h-5 text-sidebar-primary" />
            <span>Retail Ops</span>
          </div>
        </div>
        
        <div className="flex-1 py-6 px-3 flex flex-col gap-1 overflow-y-auto">
          {navItems.map((item) => {
            const isActive = location === item.href || (item.href !== "/" && location.startsWith(item.href));
            return (
              <Link 
                key={item.href} 
                href={item.href}
                className={cn(
                  "flex items-center gap-3 px-3 py-2 rounded-md text-sm font-medium transition-colors",
                  isActive 
                    ? "bg-sidebar-accent text-sidebar-accent-foreground" 
                    : "text-sidebar-foreground/70 hover:bg-sidebar-accent/50 hover:text-sidebar-foreground"
                )}
              >
                <item.icon className={cn("w-4 h-4", isActive ? "text-sidebar-primary" : "text-sidebar-foreground/50")} />
                {item.label}
              </Link>
            );
          })}
          
          <div className="mt-8 mb-2 px-4 text-xs font-semibold text-sidebar-foreground/40 uppercase tracking-wider">
            Quick Actions
          </div>
          <Link 
            href="/items/new"
            className="flex items-center gap-3 px-3 py-2 rounded-md text-sm font-medium text-sidebar-foreground/70 hover:bg-sidebar-accent/50 hover:text-sidebar-foreground transition-colors"
          >
            <Plus className="w-4 h-4 text-sidebar-foreground/50" />
            Add Item
          </Link>
        </div>
        
        <div className="p-4 border-t border-sidebar-border">
          <div className="flex items-center gap-3 px-3 py-2">
            <div className="w-8 h-8 rounded-full bg-sidebar-primary/20 flex items-center justify-center text-sidebar-primary text-xs font-bold">
              AM
            </div>
            <div className="flex flex-col">
              <span className="text-sm font-medium text-sidebar-foreground">Admin Manager</span>
              <span className="text-xs text-sidebar-foreground/50">Store 014</span>
            </div>
          </div>
        </div>
      </aside>
      
      <main className="flex-1 flex flex-col min-w-0 overflow-hidden">
        {children}
      </main>
    </div>
  );
}
