# Ideas, the Monologue. Part 1

## Functionality

## Behind the Scenes
Android Best Practices		

## Interface Design Thesis and Summary

The interface will be a single page that takes advantage of built in Android accessiblity features (TalkBack) and potentially custom elements (rendering buttons/Talkback regions over the OpenGL interface). This may be supplanted by overlaying nearly transparent buttons from a different layout, although the custom stuff is the coolest. To start with, the app will not capture custom gestures, and will instead provide feedback via TalkBack and dynamic context descriptions.

The app will provide spoken feedback and vibrational feedback. For example: spoken feedback might say "There is an obstacle abou 10 feet in front of you". Vibrational feedback may vibrate lightly for a nearby frontal obstacle and more heavily for an obstacle that the user may run into soon.

The app interface will consist of 5 major regions. The center region is the largest and provides feedback about obstacles in a frontal cone in front of the user that are immediately relevant to the user. The left and right regions at the edge of the screen provide information about the cones to the left and right of the user. The top and bottom regions provide information about further in front of the user and behind the user respectively. The feedback falls into 3 categories: unknown/not seen/not processed, occupied or known free. Occupied regions provide feedback about the estimated distance to the obstacle.

There are two main ways of exploring content in the app using TalkBack. First is touch exploration. The user drags their finger on the screen. As UI elements are highlighted, they provide audio feedback. The second exploration method is linear exploration. Users swipe left and right to explore items in a linear ordering on the page (plus groupings). Touch exploration will allow for a full range of feedback. Linear exploration may only provide feedback about the left, right and front of the user.

A stretch goal is to identify humans and provide a heartbeat feedback when a person is near the user.

### Accesible Apps for Blind and Visually Impaired Users
https://www.youtube.com/watch?v=1by5J7c5Vz4

Talk Back Screen Reader

Users use preset gestures and get audio spoken words back as a result.

2 gesture input modes:
Touch exploration: drag finger across screen, getting feedback whenever you "hit" something/an item. Double tap to activate.
Linear Navigation: swipe left and right through selections until you find the item of interest. Double tap to activate.

Attach alternative text to UI elements to be spoken by TalkBack using android:contentDescription

Use "@null" to tell TalkBack to ignore the content description and not speak for that element

Don't include state or object description (ex. Button) in description because Android does this for you

Android Lint shows which content lacks descriptions

Arrange content into logical groups by using focusable containers, including grouping rows in tables for example. Then there is just one description for the group (unless it is selected, etc).

Manually test the app with Talkback, with eyes closed.

This is important.
When drawing app window with OpenGL, manually define Accessibility data. The easiest way to achieve this goal is to rely on the explore by touch helper class. This will help build a hierarchy of views that are accessible to Talkback.

https://developer.android.com/guide/topics/ui/accessibility/apps.html?utm_campaign=android_update_appsaccessibility_051717&utm_source=anddev&utm_medium=yt-desc

Content Description can be set in code. Use this to change the content description based on the region of the screen that the user is touching. For example, if their finger is on the center of the screen or dragged to the center of the screen, estimate the distance to an obstacle in front of the user. If they drag to the left or right, indicate if there are known obstacles in the left or right quadrant. Up and down indicate far forward or behind the user. 
This would be a good place to indicate a heartbeat if the app thinks there are people in each quadrant.

### Simple Gesture Detector
https://developer.android.com/reference/android/view/GestureDetector.SimpleOnGestureListener.html

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
