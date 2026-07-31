package com.ramsiers.granitequartz.v2

private fun page(
    title: String,
    type: PageType = PageType.TEXT,
    enabled: Boolean = true,
    required: Boolean = false,
    help: String = "",
    unit: String = "",
    price: Double = 0.0,
    choices: List<Choice> = emptyList()
) = FormPage(
    title = title,
    type = type,
    enabled = enabled,
    required = required,
    helpText = help,
    unit = unit,
    price = price,
    choices = choices
)

private fun choice(
    name: String,
    price: Double = 0.0,
    unit: String = "each",
    imageUri: String = ""
) = Choice(name = name, price = price, unit = unit, imageUri = imageUri)

object Defaults {
    fun setup() = AppSetup(
        pages = listOf(
            page("Customer name", required = true),
            page("Phone number", required = true),
            page("Email address"),
            page("Job address"),
            page(
                "Choose a sink",
                PageType.PRODUCT,
                choices = listOf(
                    choice(
                        "Rectangle vanity sink - White",
                        imageUri = "android.resource://com.ramsiers.granitequartz.v2/drawable/bathroom_sink_rectangle"
                    ),
                    choice(
                        "Rectangle vanity sink - Biscuit",
                        imageUri = "android.resource://com.ramsiers.granitequartz.v2/drawable/bathroom_sink_rectangle"
                    ),
                    choice(
                        "Oval vanity sink - White",
                        imageUri = "android.resource://com.ramsiers.granitequartz.v2/drawable/bathroom_sink_oval"
                    ),
                    choice(
                        "Oval vanity sink - Biscuit",
                        imageUri = "android.resource://com.ramsiers.granitequartz.v2/drawable/bathroom_sink_oval"
                    ),
                    choice("Another sink — I want to pick my own")
                )
            ),
            page("Cooktop or extra cutouts", PageType.NUMBER, unit = "cutouts", price = 100.0),
            page(
                "Add a RAMSIER'S faucet?",
                PageType.PRODUCT,
                choices = listOf(choice("No faucet"), choice("RAMSIER'S faucet", 225.0))
            ),
            page(
                "Basket drains",
                PageType.PRODUCT,
                choices = listOf(choice("No basket drain"), choice("Basket drain", 35.0))
            ),
            page(
                "Big sink grids",
                PageType.PRODUCT,
                choices = listOf(choice("No grid"), choice("Big grid", 70.0))
            ),
            page("Are the cabinets installed?", PageType.YES_NO, required = true),
            page("Approximate cabinet installation date", PageType.DATE),
            page(
                "Would you like to buy cabinets from RAMSIER'S?",
                PageType.YES_NO,
                required = true
            ),
            page("Are there any other projects you would like a quote for?"),
            page(
                "Countertop section measurements",
                PageType.MEASUREMENT,
                enabled = false,
                required = true,
                help = "Enter length and width in inches plus the number of matching pieces.",
                unit = "sq ft",
                price = 0.0
            ),
            page("Add a kitchen or countertop photo", PageType.PHOTO),
            page(
                "Choose the edge detail",
                PageType.PRODUCT,
                choices = listOf(
                    choice("Eased and polished", 0.0, "linear foot"),
                    choice("Small round", 10.0, "linear foot"),
                    choice("Big round", 10.0, "linear foot"),
                    choice("Bevel", 10.0, "linear foot"),
                    choice("Big bevel", 10.0, "linear foot")
                )
            ),
            page(
                "MSI color or slab name",
                help = "Roomvo: https://www.roomvo.com/my/msi/?product_type=1&multi_product_visualizer=5"
            ),
            page(
                "Scan the MSI slab QR code",
                PageType.QR_SCAN,
                help = "Scan the slab tag or enter its code manually."
            ),
            page("Notes"),
            page("Review and send the quote", PageType.SUMMARY)
        )
    )
}
