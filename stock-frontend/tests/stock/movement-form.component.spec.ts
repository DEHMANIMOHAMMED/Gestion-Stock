import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { MovementFormComponent } from '../../src/app/stock/movement-form.component';
import { StockService } from '../../src/app/stock/stock.service';

describe('MovementFormComponent', () => {
  let fixture: ComponentFixture<MovementFormComponent>;
  let component: MovementFormComponent;
  let stockService: jasmine.SpyObj<StockService>;

  beforeEach(async () => {
    stockService = jasmine.createSpyObj<StockService>('StockService', ['registerMovement']);

    await TestBed.configureTestingModule({
      imports: [MovementFormComponent],
      providers: [{ provide: StockService, useValue: stockService }]
    }).compileComponents();

    fixture = TestBed.createComponent(MovementFormComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('productId', 42);
    fixture.detectChanges();
  });

  it('registers a movement with a numeric quantity', () => {
    stockService.registerMovement.and.returnValue(of(void 0));
    spyOn(component.closed, 'emit');

    component.form.patchValue({
      quantity: 7,
      type: 'IN'
    });

    component.save();

    expect(stockService.registerMovement).toHaveBeenCalledOnceWith({
      productId: 42,
      quantity: 7,
      type: 'IN'
    });
    expect(component.closed.emit).toHaveBeenCalled();
  });

  it('keeps the form open and shows backend errors', () => {
    stockService.registerMovement.and.returnValue(throwError(() => ({
      error: { detail: 'Insufficient stock' }
    })));
    spyOn(component.closed, 'emit');

    component.form.patchValue({
      quantity: 10,
      type: 'OUT'
    });

    component.save();
    fixture.detectChanges();

    expect(component.closed.emit).not.toHaveBeenCalled();
    expect(component.message).toBe('Insufficient stock');
    expect(fixture.nativeElement.textContent).toContain('Insufficient stock');
  });
});
