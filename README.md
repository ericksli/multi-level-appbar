# Multi-level Expandable App Bar

This is a demo of two-level expandable app bar. In the first level, it shows an image like the
regular usage of `CollapsingToolbarLayout`. When user tapping or scrolling down on the semi-expanded
toolbar, it will further expand the toolbar to show some detail information in a `NestedScrollView`.
When the toolbar is fully expanded, user may scroll to the bottom of the text or tap
the close button to collapse the toolbar.

I used `CollapsingToolbarLayout` from Android Design Support Library as the base and add different
listeners/detectors to detect scroll and tap events.

Watch the [demo video](https://www.youtube.com/watch?v=oJ1u-KyrjLw) for details.
