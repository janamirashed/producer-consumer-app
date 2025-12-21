import { ComponentFixture, TestBed } from '@angular/core/testing';

import { QueueDisplay } from './queue-display';

describe('QueueDisplay', () => {
  let component: QueueDisplay;
  let fixture: ComponentFixture<QueueDisplay>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QueueDisplay]
    })
    .compileComponents();

    fixture = TestBed.createComponent(QueueDisplay);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
