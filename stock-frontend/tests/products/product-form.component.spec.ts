import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ProductFormComponent } from '../../src/app/products/product-form.component';
import { ProductService, Product } from '../../src/app/products/product.service';

describe('ProductFormComponent', () => {
  let fixture: ComponentFixture<ProductFormComponent>;
  let component: ProductFormComponent;
  let productService: jasmine.SpyObj<ProductService>;

  const emptyProduct: Product = {
    id: 0,
    organisationId: 0,
    name: '',
    sku: '',
    category: '',
    minStock: 0,
    unit: ''
  };

  beforeEach(async () => {
    productService = jasmine.createSpyObj<ProductService>('ProductService', ['create', 'update']);

    await TestBed.configureTestingModule({
      imports: [ProductFormComponent],
      providers: [{ provide: ProductService, useValue: productService }]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductFormComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('product', emptyProduct);
    fixture.detectChanges();
  });

  it('creates a product with a numeric minStock payload', () => {
    productService.create.and.returnValue(of({
      id: 1,
      organisationId: 2,
      name: 'Cafe',
      sku: 'CAFE-1',
      category: 'Boissons',
      minStock: 5,
      unit: 'pcs'
    }));

    component.form.patchValue({
      name: 'Cafe',
      sku: 'CAFE-1',
      category: 'Boissons',
      minStock: 5,
      unit: 'pcs'
    });
    spyOn(component.saved, 'emit');

    component.submit();

    expect(productService.create).toHaveBeenCalledOnceWith({
      name: 'Cafe',
      sku: 'CAFE-1',
      category: 'Boissons',
      minStock: 5,
      unit: 'pcs'
    });
    expect(component.saved.emit).toHaveBeenCalled();
  });

  it('shows backend problem details when creation fails', () => {
    productService.create.and.returnValue(throwError(() => ({
      error: JSON.stringify({ detail: 'SKU already exists' })
    })));

    component.form.patchValue({
      name: 'Cafe',
      sku: 'CAFE-1',
      category: 'Boissons',
      minStock: 5,
      unit: 'pcs'
    });

    component.submit();
    fixture.detectChanges();

    expect(component.message).toBe('SKU already exists');
    expect(fixture.nativeElement.textContent).toContain('SKU already exists');
  });
});
