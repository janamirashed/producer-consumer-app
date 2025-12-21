import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SimulationCanvas } from './simulation-canvas';

describe('SimulationCanvas', () => {
  let component: SimulationCanvas;
  let fixture: ComponentFixture<SimulationCanvas>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SimulationCanvas]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SimulationCanvas);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
