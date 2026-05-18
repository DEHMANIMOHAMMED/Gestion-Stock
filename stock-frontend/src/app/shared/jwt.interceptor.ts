import { HttpInterceptorFn } from '@angular/common/http';

const PUBLIC_ENDPOINTS = [
  '/auth/login',
  '/auth/register'
];

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const isPublic = PUBLIC_ENDPOINTS.some(url =>
    req.url.includes(url)
  );

  if (isPublic) {
    return next(req);
  }

  const token = localStorage.getItem('jwt');

  if (!token) {
    return next(req);
  }

  const authReq = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });

  return next(authReq);
};
