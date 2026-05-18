import { bootstrapApplication } from '@angular/platform-browser';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors, HTTP_INTERCEPTORS } from '@angular/common/http';
import { AppComponent } from './app/app.component';
import { routes } from './app/app.routes';
import { JwtInterceptor } from './app/core/interceptors/jwt.interceptor';

bootstrapApplication(AppComponent, {
  providers: [
    provideAnimations(),
    provideRouter(routes),
    provideHttpClient(),
    { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true },
  ],
}).catch((err) => console.error(err));
