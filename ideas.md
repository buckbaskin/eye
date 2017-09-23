# Ideas, the Monologue. Part 1

## Functionality

## Behind the Scenes

## Interface

### Simple Gesture Detector

This has a few simple options to handle. These can/will be used to make up a simple to use interface for interacting with the app without looking at the app (although if the app does pretty things while we're looking at it, that's great too for a demo).

- context click (equivalent to right click, long press to pull up a context menu)
- double tap
- double tap events (that occur during a double tap, ex. down, move and up events)
- down motion event
- fling?
- long press (how is this different from context click?)
- scroll (I don't think there's going to be scrolling in the app/single page app)
- show press (user has performed a down event and not moved or performed an up)
- single tap confirmed
- single tap up. Notified when a tap occurs with the up motion that triggered it

#### Down
This is the finger hitting the screen event I believe.

#### Up
This is the finger leaving the screen event I believe.

#### Double Tap
Duh

#### Double Tap Events
What is a move event inside a double tap? Move is a translation along the screen.

#### Fling
Touch the screen, move finger and release in a flick. Gives velocity in x and y for scrolling and that kind of thing.

#### Long Press
Duh. You get the initial on down event.

#### Scroll
Duh. When moving downed finger but not flicking it.

#### Show Press
What is a show press? Is it like a drill press? This is an event that goes off when the user goes down, but hasn't continued yet (release, move, etc). Use this to do something to indicate that the action was recognized, and I'm waiting for their finger to do something. ex highlight a button that is about to be clicked on release.

#### Single Tap Up vs. Confirmed
Confirmed is the input guarunteeing the single tap won't turn into a double tap. The up is the first indication that a single tap has happened, and may turn into a double tap.
