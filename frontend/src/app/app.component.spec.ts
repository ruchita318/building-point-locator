import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { AppComponent } from './app.component';
import { LocationResponse, LocationService } from './location.service';

describe('AppComponent', () => {
  let fixture: ComponentFixture<AppComponent>;
  let component: AppComponent;
  let service: jasmine.SpyObj<LocationService>;

  beforeEach(async () => {
    service = jasmine.createSpyObj<LocationService>('LocationService', ['locate']);
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [{ provide: LocationService, useValue: service }]
    }).compileComponents();
    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('renders coordinate form', () => {
    expect(fixture.nativeElement.textContent).toContain('Find a building & floor');
    expect(fixture.nativeElement.querySelectorAll('input').length).toBe(3);
  });

  it('shows successful location with the response message', () => {
    service.locate.and.returnValue(of({
      found: true,
      building: 'Office building',
      floor: 'Floor 1',
      message: 'Point is inside Office building, Floor 1.'
    }));

    component.locate();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('3D point is in building Office building and floor Floor 1');
    expect(fixture.nativeElement.textContent).toContain('Point is inside Office building, Floor 1.');
  });

  it('submits edited coordinates and shows loading state until the response arrives', fakeAsync(() => {
    const response = new Subject<LocationResponse>();
    service.locate.and.returnValue(response.asObservable());
    const [xInput, yInput, zInput] = fixture.nativeElement.querySelectorAll('input') as NodeListOf<HTMLInputElement>;

    setInputValue(xInput, '42.5');
    setInputValue(yInput, '7.25');
    setInputValue(zInput, '-3');
    tick();
    fixture.detectChanges();

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    expect(service.locate).toHaveBeenCalledWith({ x: 42.5, y: 7.25, z: -3 });
    expect(button.disabled).toBeTrue();
    expect(button.textContent).toContain('Searching');

    response.next({
      found: true,
      building: 'Office building',
      floor: 'Floor -1',
      message: 'Point is inside Office building, Floor -1.'
    });
    response.complete();
    fixture.detectChanges();

    expect(button.disabled).toBeFalse();
    expect(fixture.nativeElement.textContent).toContain('Floor -1');
  }));

  it('shows not found state from a successful observable response', () => {
    service.locate.and.returnValue(of({
      found: false,
      building: null,
      floor: null,
      message: 'The point is not inside any building floor.'
    }));

    component.locate();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Not found');
    expect(fixture.nativeElement.textContent).toContain('The point is not inside any building floor.');
  });

  it('shows not found state from LocationController 404 response body', () => {
    service.locate.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 404,
      error: {
        found: false,
        building: null,
        floor: null,
        message: 'The point is not inside any building floor.'
      }
    })));

    component.locate();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Not found');
    expect(fixture.nativeElement.textContent).toContain('The point is not inside any building floor.');
    expect(fixture.nativeElement.textContent).not.toContain('Unable to query the location service. Please try again.');
  });

  it('shows LocationController validation error messages', () => {
    service.locate.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 400,
      error: {
        status: 400,
        error: 'Bad Request',
        message: 'z must not be null'
      }
    })));

    component.locate();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('z must not be null');
  });

  it('shows LocationController server error messages', () => {
    service.locate.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 500,
      error: {
        status: 500,
        error: 'Internal Server Error',
        message: 'Unable to process the location lookup request.'
      }
    })));

    component.locate();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Unable to process the location lookup request.');
  });

  it('shows friendly API error for non-controller failures', () => {
    service.locate.and.returnValue(throwError(() => new Error('network')));

    component.locate();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Unable to query the location service. Please try again.');
  });

  function setInputValue(input: HTMLInputElement, value: string): void {
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }
});
