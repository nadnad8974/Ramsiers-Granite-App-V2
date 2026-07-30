# RAMSIER'S GRANITE AND QUARTZ V2

A new Android app built separately from `nadnad8974/countertoptop-APP2` so the working V1 app remains untouched during development.

## Main goal

Build a data-driven Android quote app with an Advanced Live Editor. Most content changes should be possible directly on the phone without rebuilding the APK.

## Live editor requirements

- Add, delete, rename, duplicate, enable, disable, and reorder pages.
- Keep the edited page in view when moving it up or down.
- Choose page types: text, number, currency, date, Yes/No, multiple choice, product selection, photo, QR scan, measurement, and summary.
- Add and replace product pictures from the phone.
- Edit choices, quantities, prices, units, formulas, help text, and required/optional rules.
- Preview changes immediately.
- Automatically save the form design and current quote.
- Export and import the complete app setup as JSON for backup.
- Provide Restore Defaults and Undo.

## Customer quote workflow

- One question per screen.
- Back and Next buttons directly below the question and always visible above the keyboard.
- Customer name, phone, email, address, notes, and office email.
- MSI slab QR scanning and manual slab entry.
- MSI links and Roomvo visualizer: https://www.roomvo.com/my/msi/?product_type=1&multi_product_visualizer=5
- Countertop section measurements, quantities, square footage, stove opening, and pricing.
- Three sink product choices with pictures.
- Cooktop or extra cutouts at $100 each.
- Edge details: eased and polished free; small round, big round, bevel, and big bevel at $10 per linear foot.
- RAMSIER'S faucet at $225.
- Basket drains at $35 each with picture.
- Big grids at $70 each with picture.
- Ask whether cabinets are installed and the approximate date.
- On the next screen ask whether the customer wants to buy cabinets from RAMSIER'S.
- Kitchen/countertop photo selection.
- Review, calculate, save, and send the quote.

## V2 testing strategy

Use a separate Android application ID so V2 can be installed beside V1. Keep V1 and its GitHub repository unchanged until V2 passes phone testing.
