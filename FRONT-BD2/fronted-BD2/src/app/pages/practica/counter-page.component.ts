import {Component} from '@angular/core';


@Component({

  template:
  `
  <div class="container">
    <h1>contador: {{counter}}</h1>
    <p>SAPOOOO</p>

  </div>
    <button (click)="sumar(1)">+1</button>

  `


})
export class CounterPageComponent{

  counter = 10

  sumar(value: number){
    this.counter += value;


  }


}
