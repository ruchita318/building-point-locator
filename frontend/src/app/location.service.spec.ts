import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { LocationService } from './location.service';

describe('LocationService', () => {
  let service: LocationService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        LocationService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(LocationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('POSTs the point and returns a match', () => {
    service.locate({ x: 15, y: 15, z: 1 }).subscribe(response => {
      expect(response.found).toBeTrue();
      expect(response.building).toBe('Office building');
    });

    const req = http.expectOne('/api/v1/location/locate');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ x: 15, y: 15, z: 1 });
    req.flush({
      found: true,
      building: 'Office building',
      floor: 'Floor 0',
      message: 'Point is inside Office building, Floor 0.'
    });
  });

  it('passes through controller not-found responses as HTTP errors', () => {
    service.locate({ x: 999, y: 999, z: 1 }).subscribe({
      next: () => fail('Expected the request to fail with a 404 response'),
      error: (error: HttpErrorResponse) => {
        expect(error.status).toBe(404);
        expect(error.error).toEqual({
          found: false,
          building: null,
          floor: null,
          message: 'The point is not inside any building floor.'
        });
      }
    });

    const req = http.expectOne('/api/v1/location/locate');
    req.flush({
      found: false,
      building: null,
      floor: null,
      message: 'The point is not inside any building floor.'
    }, {
      status: 404,
      statusText: 'Not Found'
    });
  });
});
