import { Component, ErrorInfo, ReactNode } from 'react';
import { ErrorBanner } from './ErrorBanner';

interface Props {
  children?: ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
}

export class ErrorBoundary extends Component<Props, State> {
  public override state: State = {
    hasError: false,
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public override componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Uncaught error in UI component:', error, errorInfo);
  }

  public override render() {
    if (this.state.hasError) {
      return (
        <div className="p-6">
          <ErrorBanner
            title="UI Render Exception"
            message={this.state.error?.message || 'An unexpected rendering error occurred.'}
          />
        </div>
      );
    }
    return this.props.children;
  }
}