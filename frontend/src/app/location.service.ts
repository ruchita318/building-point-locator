import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Point3D { x: number; y: number; z: number; }
export interface LocationResponse {
  found: boolean;
  building: string | null;
  floor: string | null;
  message: string;
}
export interface ApiErrorResponse {
  status: number;
  error: string;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class LocationService {
  private readonly http = inject(HttpClient);

  locate(point: Point3D): Observable<LocationResponse> {
    return this.http.post<LocationResponse>('/api/locate', point);
  }
}
