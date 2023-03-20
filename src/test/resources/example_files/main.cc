
/* Add your code here */
#pragma once
#include "machine/pic.h"
#include "machine/cpu.h"
#include "machine/plugbox.h"
#include "device/keyboard.h"
#include "object/queue.h"
#include "guard/guard.h"
#include "thread/scheduler.h"
#include "user/loop.h"
#include "device/watch.h"
#include "syscall/guarded_organizer.h"
#include "syscall/guarded_semaphore.h"
#include "syscall/guarded_keyboard.h"
#include "syscall/guarded_buzzer.h"
#include "syscall/guarded_stream.h"
#include "meeting/bellringer.h"
#include "user/test.h"
#include "user/keyboardInteract.h"
#include "user/buzzerTest.h"
// global variables
Guarded_Stream kout;
Plugbox pb;
CPU cpu;
PIC pic;
Guarded_Keyboard keyboard;
Queue q;
Guard guard;
int timerfrequency = 10000;
Watch timer(timerfrequency);//54925 max=55ms sets timer to 0xFFFF. =2^16.
Guarded_Organizer organizer;
Guarded_Semaphore s(1);
Bellringer bellringer;
Guarded_Buzzer buzzer1;
Guarded_Buzzer buzzer2;
int contineous_ticks=0;



int entrie=1;
unsigned char stack[65536];
int stack_size = 2048;
int currentstackframe=65536; 
char * stackpointer=(char *)&stack[65536];
void * stackpointerdown;
void * stackpointerup;

int main()
{
	keyboard.plugin();
	cpu.enable_int();
	
	//Loop loop1(stackpointer, 20, 20);
	//organizer.ready(loop1);
	Test test1(stackpointer, 20, 20);
	organizer.ready(test1);
	stackpointer-=stack_size;

	// Loop loop2(stackpointer, 30, 20);
	// organizer.ready(loop2);
	Test test2(stackpointer, 30, 20);
	organizer.ready(test2);
	stackpointer-=stack_size;

	// Loop loop3(stackpointer, 40, 20);
	// organizer.ready(loop3);
	Test test3(stackpointer, 40, 20);
	organizer.ready(test3);
	stackpointer-=stack_size;

	// Loop loop4(stackpointer, 50, 20);
	// organizer.ready(loop4);
	Test test4(stackpointer, 50, 20);
	organizer.ready(test4);
	stackpointer-=stack_size;

	keyboardInteract kb(stackpointer, 15, 15);
	organizer.ready(kb);
	stackpointer-=stack_size;
	buzzerTest buzz1_0(stackpointer,10,10000, 25, 15);
	organizer.ready(buzz1_0);
	stackpointer-=stack_size;
	buzzerTest buzz1_1(stackpointer,60,1000, 40, 15);
	organizer.ready(buzz1_1);
	stackpointer-=stack_size;
	buzzerTest buzz1_2(stackpointer,100,10, 60, 15);
	organizer.ready(buzz1_2);
	stackpointer-=stack_size;

	guard.enter();
	timer.windup();
	organizer.schedule();
	
	return 0;
}                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         