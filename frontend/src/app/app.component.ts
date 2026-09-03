import { Component, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ApiErrorResponse, LocationResponse, LocationService, Point3D } from './location.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [FormsModule],
  template: `
    <main class="page">
      <section class="card">
        <div class="eyebrow">3D GEOLOCATION</div>
        <h1>Find a building & floor</h1>
        <p class="subtitle">Enter a point using metric X, Y and Z coordinates.</p>

        <form (ngSubmit)="locate()" #form="ngForm">
          <div class="coordinates">
            <label>X <input name="x" type="number" step="any" [(ngModel)]="point.x" required></label>
            <label>Y <input name="y" type="number" step="any" [(ngModel)]="point.y" required></label>
            <label>Z <input name="z" type="number" step="any" [(ngModel)]="point.z" required></label>
          </div>
          <button type="submit" [disabled]="form.invalid || loading">
            {{ loading ? 'Searching…' : 'Find location' }}
          </button>
        </form>

        @if (result) {
          <div class="result" [class.success]="result.found" [class.empty]="!result.found">
            @if (result.found) {
              <span class="label">LOCATION FOUND</span>
              <strong>3D point is in building {{ result.building }} and floor {{ result.floor }}</strong>
              <span class="message">{{ result.message }}</span>
            } @else {
              <strong>Not found</strong>
              <span>{{ result.message }}</span>
            }
          </div>
        }

        @if (error) {
          <div class="error">{{ error }}</div>
        }
      </section>
    </main>
  `,
  styles: [`
    :host { display:block; min-height:100vh; }
    .page { min-height:100vh; display:grid; place-items:center; padding:24px; box-sizing:border-box; }
    .card { width:min(600px,100%); padding:36px; border-radius:22px; background:rgba(255,255,255,.96); box-shadow:0 20px 60px rgba(15,23,42,.12); }
    .eyebrow { font-size:12px; letter-spacing:.14em; font-weight:800; opacity:.65; }
    h1 { margin:8px 0 8px; font-size:32px; }
    .subtitle { color:#667085; margin:0 0 28px; }
    .coordinates { display:grid; grid-template-columns:repeat(3,1fr); gap:14px; }
    label { font-weight:700; }
    input { width:100%; box-sizing:border-box; margin-top:7px; padding:12px; border:1px solid #d0d5dd; border-radius:10px; font-size:16px; }
    button { width:100%; border:0; border-radius:10px; margin-top:18px; padding:13px; font-size:16px; font-weight:800; cursor:pointer; background:#175cd3; color:white; }
    button:disabled { opacity:.55; cursor:wait; }
    .result { display:flex; flex-direction:column; gap:5px; margin-top:22px; padding:18px; border-radius:12px; }
    .result.success { background:#ecfdf3; color:#067647; }
    .result.empty { background:#fef3f2; color:#b42318; }
    .result strong { font-size:21px; }
    .message { line-height:1.45; }
    .label { font-size:11px; font-weight:900; letter-spacing:.1em; }
    .error { margin-top:16px; padding:14px; border-radius:10px; background:#fef3f2; color:#b42318; }
    @media (max-width:520px) { .card{padding:24px;} .coordinates{grid-template-columns:1fr;} h1{font-size:27px;} }
  `]
})
export class AppComponent {
  private readonly locationService = inject(LocationService);
  point: Point3D = { x: 15, y: 15, z: 1 };
  result: LocationResponse | null = null;
  loading = false;
  error = '';

  locate(): void {
    this.loading = true;
    this.result = null;
    this.error = '';
    this.locationService.locate(this.point).subscribe({
      next: value => { this.result = value; this.loading = false; },
      error: err => this.handleLocateError(err)
    });
  }

  private handleLocateError(err: unknown): void {
    this.loading = false;

    if (err instanceof HttpErrorResponse) {
      if (this.isLocationResponse(err.error)) {
        this.result = err.error;
        return;
      }

      if (this.isApiErrorResponse(err.error)) {
        this.error = err.error.message;
        return;
      }
    }

    this.error = 'Unable to query the location service. Please try again.';
  }

  private isLocationResponse(value: unknown): value is LocationResponse {
    if (!this.isRecord(value)) {
      return false;
    }

    return typeof value['found'] === 'boolean'
      && this.isNullableString(value['building'])
      && this.isNullableString(value['floor'])
      && typeof value['message'] === 'string';
  }

  private isApiErrorResponse(value: unknown): value is ApiErrorResponse {
    if (!this.isRecord(value)) {
      return false;
    }

    return typeof value['status'] === 'number'
      && typeof value['error'] === 'string'
      && typeof value['message'] === 'string';
  }

  private isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null;
  }

  private isNullableString(value: unknown): value is string | null {
    return typeof value === 'string' || value === null;
  }
}
