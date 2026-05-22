import { Injectable } from '@angular/core';
import { environment } from '../environment';
import { runtimeGoogleClientId } from '../runtime-config';

interface GoogleCredentialResponse {
  credential?: string;
}

interface GoogleAccounts {
  id: {
    initialize(config: { client_id: string; callback: (response: GoogleCredentialResponse) => void }): void;
    renderButton(element: HTMLElement, options: { theme: string; size: string; width: number; text: string }): void;
    cancel?(): void;
  };
}

declare global {
  interface Window {
    google?: {
      accounts: GoogleAccounts;
    };
  }
}

@Injectable({ providedIn: 'root' })
export class GoogleIdentityService {
  private scriptPromise?: Promise<void>;

  renderButton(
    elementId: string,
    buttonText: 'signin_with' | 'signup_with',
    callback: (idToken: string) => void
  ): Promise<void> {
    return this.loadScript().then(() => {
      const target = document.getElementById(elementId);
      if (!target || !window.google) {
        return;
      }

      const clientId = runtimeGoogleClientId() || environment.googleClientId;
      if (!clientId) {
        target.textContent = 'Connexion Google non configuree';
        return;
      }

      target.replaceChildren();
      window.google.accounts.id.initialize({
        client_id: clientId,
        callback: (response) => {
          if (response.credential) {
            callback(response.credential);
          }
        }
      });

      window.google.accounts.id.renderButton(target, {
        theme: 'outline',
        size: 'large',
        width: 320,
        text: buttonText
      });
    });
  }

  cancel(): void {
    window.google?.accounts.id.cancel?.();
  }

  private loadScript(): Promise<void> {
    if (window.google?.accounts?.id) {
      return Promise.resolve();
    }

    if (this.scriptPromise) {
      return this.scriptPromise;
    }

    this.scriptPromise = new Promise<void>((resolve, reject) => {
      const existingScript = document.getElementById('google-identity-script') as HTMLScriptElement | null;
      if (existingScript) {
        existingScript.addEventListener('load', () => resolve(), { once: true });
        existingScript.addEventListener('error', () => reject(new Error('Google Identity script failed to load')), { once: true });
        return;
      }

      const script = document.createElement('script');
      script.id = 'google-identity-script';
      script.src = 'https://accounts.google.com/gsi/client';
      script.async = true;
      script.defer = true;
      script.onload = () => resolve();
      script.onerror = () => reject(new Error('Google Identity script failed to load'));
      document.head.appendChild(script);
    });

    return this.scriptPromise;
  }
}
