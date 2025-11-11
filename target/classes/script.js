// const createBooking = function (flightNum,numPassengers=1, price=199 * numPassengers){
//     // numPassengers = numPassengers || 1;
//     // price = price || 200;
//
//     const booking = {
//         flightNum,
//         numPassengers,
//         price
//     }
//
//     console.log(booking);
// }
//
// createBooking('LH123');
// createBooking('LH123',2,800);
// createBooking('LH123',2);
// createBooking('LH123',5);
// createBooking('LH123',undefined,1000);
//
// const flight = 'LH234';
// const rahul = {
//     name: 'Rahul Vijay',
//     passport: 24739479284
// };
//
// const checkIn = function(flightNum, passenger){
//     flightNum = 'LH999';
//     passenger.name = 'Mr. ' + passenger.name;
//
//     if(passenger.passport === 24739479284){
//         console.log('Checked in');    } else {
//         console.log('Wrong passport!');
//     }
// }
//
// checkIn(flight, rahul);
// console.log(flight);
// console.log(rahul);
//
// const newPassport = function(person){
//     person.passport = Math.trunc(Math.random() * 100000000000);
// }
//
// newPassport(rahul);
// checkIn(flight, rahul);
//
// const oneWord = function(str){
//     const noSpaces = str.split(' ').join('');
//     return noSpaces.toLowerCase();
// }
//
// const upperAllWords = function(str){
//     return str.toUpperCase();
// }
//
// //Higher-order function
// const transformer = function(str, fn){
//     console.log(`Original string: ${str}`);
//     console.log(`Transformed string: ${fn(str)}`);
//     console.log(`Transformed by: ${fn.name}`);
// }
// transformer('JavaScript is the best!', oneWord);
// transformer('JavaScript is the best!', upperAllWords);
//
//
// const greet=function(greeting){
//     return function(name){
//         console.log(`${greeting} ${name}`);
//     }
// }

// const greet1 = greet("Hey");
// greet1("Rahul");

// greet("hi")("Vijay");
//
// const greetArr = greeting => name => console.log(`${greeting} ${name}`);
//
// greetArr("Hello")("Rahul");

const owners = ['Rahul','Suresh','Kiran'];
console.log(owners.sort())
console.log(owners);

const arr = [200,450,-400,3000,-650,-130,70,1300];
console.log(arr.sort((a, b) => a - b));
console.log(arr);

const str= 'hello world world';
const s = str.replaceAll('world','rahul');
const s1 = str.replace('world','rahul');
console.log(s,'\n'+s1);