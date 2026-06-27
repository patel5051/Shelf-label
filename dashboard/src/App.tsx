import { Switch, Route, Router as WouterRouter } from "wouter";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Toaster } from "@/components/ui/toaster";
import { TooltipProvider } from "@/components/ui/tooltip";
import NotFound from "@/pages/not-found";
import { Layout } from "@/components/layout/layout";

import DashboardPage from "@/pages/dashboard";
import ItemsPage from "@/pages/items/index";
import NewItemPage from "@/pages/items/new";
import EditItemPage from "@/pages/items/edit";
import PrintHistoryPage from "@/pages/print-history/index";
import UploadPage from "@/pages/upload/index";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

function Router() {
  return (
    <Layout>
      <Switch>
        <Route path="/" component={DashboardPage} />
        <Route path="/items" component={ItemsPage} />
        <Route path="/items/new" component={NewItemPage} />
        <Route path="/items/:id/edit" component={EditItemPage} />
        <Route path="/print-history" component={PrintHistoryPage} />
        <Route path="/upload" component={UploadPage} />
        <Route component={NotFound} />
      </Switch>
    </Layout>
  );
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <TooltipProvider>
        <WouterRouter base={import.meta.env.BASE_URL.replace(/\/$/, "")}>
          <Router />
        </WouterRouter>
        <Toaster />
      </TooltipProvider>
    </QueryClientProvider>
  );
}

export default App;
