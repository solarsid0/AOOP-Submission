package oop.classes.empselfservice;

import java.io.ByteArrayInputStream;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorker;
import com.itextpdf.tool.xml.XMLWorkerFontProvider;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import com.itextpdf.tool.xml.css.CssFile;
import com.itextpdf.tool.xml.css.StyleAttrCSSResolver;
import com.itextpdf.tool.xml.html.CssAppliers;
import com.itextpdf.tool.xml.html.CssAppliersImpl;
import com.itextpdf.tool.xml.html.Tags;
import com.itextpdf.tool.xml.parser.XMLParser;
import com.itextpdf.tool.xml.pipeline.css.CSSResolver;
import com.itextpdf.tool.xml.pipeline.css.CssResolverPipeline;
import com.itextpdf.tool.xml.pipeline.end.PdfWriterPipeline;
import com.itextpdf.tool.xml.pipeline.html.HtmlPipeline;
import com.itextpdf.tool.xml.pipeline.html.HtmlPipelineContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import oop.classes.calculations.PayrollSummary;
import oop.classes.actors.Employee;

/**
 * Payslip class inherits payroll details from PayrollSummary and provides a method to display employee payslip.
 * Payslip uses HTML format and saved as PDF (will automatically save on "Downloads" folder).
 * This class is ONLY responsible for display and formatting, not calculations.
 * @author Admin
 */
public class Payslip extends PayrollSummary {
    
    // Class variables
    private YearMonth payrollMonth;
    
    // Company colors
    private static final String COMPANY_RED = "#CF0A0A";
    private static final String COMPANY_LIGHT_RED = "#FFE6E6";
    private static final String COMPANY_DARK_RED = "#960404";
    
    /**
     * Constructor - creates a new payslip for an employee
     * @param employee
     */
    public Payslip(Employee employee) {
        super(employee); // Call the PayrollSummary constructor with Employee object
        this.payrollMonth = YearMonth.now(); // Default to current month
    }

    /**
     * Formats a number as Philippine Peso
     */
    private String formatAsPhp(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("tl", "PH")); // "tl" is Tagalog
        return format.format(amount);
    }

    /**
     * Generates and saves a PDF payslip for the specified month and year
     * @param month
     * @param year
     * @throws java.io.IOException
     * @throws com.itextpdf.text.DocumentException
     */
    public void printPayslip(int month, int year) throws IOException, DocumentException {
        // Format the month and year for display
        DateTimeFormatter monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy");
        LocalDate payslipDate = LocalDate.of(year, month, 1);
        String period = payslipDate.format(monthYearFormatter);

        // Build the HTML content for the payslip
        StringBuilder htmlContent = new StringBuilder();
        
        // HTML header and styling
        htmlContent.append("<!DOCTYPE html>\n");
        htmlContent.append("<html lang=\"en\">\n");
        htmlContent.append("<head>\n");
        htmlContent.append("    <meta charset=\"UTF-8\"/>\n");
        htmlContent.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>\n");
        htmlContent.append("    <title>Payslip</title>\n");
        htmlContent.append("    <style>\n");
        // Basic layout and colors
        htmlContent.append("        body {\n");
        htmlContent.append("            font-family: Arial, sans-serif;\n");
        htmlContent.append("            background-color: #FFFFFF;\n");
        htmlContent.append("            margin: 0;\n");
        htmlContent.append("            padding: 0;\n");
        htmlContent.append("            font-size: 12px;\n");
        htmlContent.append("            width: 100%;\n");
        htmlContent.append("        }\n");
        htmlContent.append("        table {\n");
        htmlContent.append("            width: 100%;\n");
        htmlContent.append("            border-collapse: collapse;\n");
        htmlContent.append("            margin-bottom: 10px;\n");
        htmlContent.append("        }\n");
        htmlContent.append("        table, th, td {\n");
        htmlContent.append("            border: 1px solid ").append(COMPANY_RED).append(";\n");
        htmlContent.append("        }\n");
        htmlContent.append("        th, td {\n");
        htmlContent.append("            padding: 8px;\n");
        htmlContent.append("            text-align: left;\n");
        htmlContent.append("        }\n");
        htmlContent.append("        th {\n");
        htmlContent.append("            background-color: ").append(COMPANY_RED).append(";\n");
        htmlContent.append("            color: white;\n");
        htmlContent.append("            font-weight: bold;\n");
        htmlContent.append("        }\n");
        htmlContent.append("        .header-cell {\n");
        htmlContent.append("            background-color: ").append(COMPANY_RED).append(";\n");
        htmlContent.append("            color: white;\n");
        htmlContent.append("            text-align: center;\n");
        htmlContent.append("            font-weight: bold;\n");
        htmlContent.append("            font-size: 16px;\n");
        htmlContent.append("            padding: 10px;\n");
        htmlContent.append("        }\n");
        htmlContent.append("        .section-title {\n");
        htmlContent.append("            background-color: ").append(COMPANY_RED).append(";\n");
        htmlContent.append("            color: white;\n");
        htmlContent.append("            font-weight: bold;\n");
        htmlContent.append("            padding: 5px;\n");
        htmlContent.append("        }\n");
        htmlContent.append("        .right-align {\n");
        htmlContent.append("            text-align: right;\n");
        htmlContent.append("        }\n");
        htmlContent.append("        .total-row {\n");
        htmlContent.append("            font-weight: bold;\n");
        htmlContent.append("            background-color: ").append(COMPANY_LIGHT_RED).append(";\n");
        htmlContent.append("        }\n");
        htmlContent.append("        .calculation-note {\n");
        htmlContent.append("            font-size: 10px;\n");
        htmlContent.append("            color: #666666;\n");
        htmlContent.append("            font-style: italic;\n");
        htmlContent.append("            padding: 3px 8px;\n");
        htmlContent.append("            background-color: #FFF9F9;\n");
        htmlContent.append("        }\n");
        htmlContent.append("        .final-total {\n");
        htmlContent.append("            font-size: 14px;\n");
        htmlContent.append("            font-weight: bold;\n");
        htmlContent.append("            background-color: ").append(COMPANY_DARK_RED).append(";\n");
        htmlContent.append("            color: white;\n");
        htmlContent.append("        }\n");
        htmlContent.append("    </style>\n");
        htmlContent.append("</head>\n");
        
        // Start of document body
        htmlContent.append("<body>\n");
        
        // Main table structure
        // Company header
        htmlContent.append("<table>\n");
        htmlContent.append("    <tr>\n");
        htmlContent.append("        <td colspan=\"2\" class=\"header-cell\">MOTORPH PAYSLIP</td>\n");
        htmlContent.append("    </tr>\n");
        htmlContent.append("</table>\n");
        
        // SECTION 1: Employee Details
        htmlContent.append("<table>\n");
        htmlContent.append("    <tr>\n");
        htmlContent.append("        <th colspan=\"2\" class=\"section-title\">EMPLOYEE DETAILS</th>\n");
        htmlContent.append("    </tr>\n");
        htmlContent.append("    <tr>\n");
        htmlContent.append("        <td width=\"50%\">Employee ID:</td>\n");
        htmlContent.append("        <td width=\"50%\">").append(getEmployeeId()).append("</td>\n");
        htmlContent.append("    </tr>\n");
        htmlContent.append("    <tr>\n");
        htmlContent.append("        <td>Name:</td>\n");
        htmlContent.append("        <td>").append(getEmployeeName()).append("</td>\n");
        htmlContent.append("    </tr>\n");
        
        // Position should be retrieved from PayrollSummary
        String position = getPosition();

        htmlContent.append("    <tr>\n");
        htmlContent.append("        <td>Position:</td>\n");
        htmlContent.append("        <td>").append(position).append("</td>\n");
        htmlContent.append("    </tr>\n");
        htmlContent.append("    <tr>\n");
        htmlContent.append("        <td>Pay Period:</td>\n");
        htmlContent.append("        <td>").append(period).append("</td>\n");
        htmlContent.append("    </tr>\n");
        htmlContent.append("</table>\n");
        
        // SECTION 2: Earnings
        double basicSalary = getBasicSalary();
        double overtimePay = getOvertimePay();
        double grossPay = getGrossSalary();
        
        htmlContent.append("<table>\n");
        htmlContent.append("    <tr>\n");
        htmlContent.append("        <th colspan=\"2\" class=\"section-title\">EARNINGS</th>\n");
        htmlContent.append("    </tr>\n");
        htmlContent.append("    <tr>\n");
        htmlContent.append("        <td width=\"70%\">Basic Salary:</td>\n");
        htmlContent.append("        <td width=\"30%\" class=\"right-align\">").append(formatAsPhp(basicSalary)).append("</td>\n");
        htmlContent.append("    </tr>\n");
        
        // Only include overtime if there is any
        if (overtimePay > 0) {
            htmlContent.append("    <tr>\n");
            htmlContent.append("        <td>Overtime:</td>\n");
            htmlContent.append("        <td class=\"right-align\">").append(formatAsPhp(overtimePay)).append("</td>\n");
            htmlContent.append("    </tr>\n");
        }
        
        htmlContent.append("    <tr class=\"total-row\">\n");
        htmlContent.append("        <td>Gross Pay:</td>\n");
        htmlContent.append("        <td class=\"right-align\">").append(formatAsPhp(grossPay)).append("</td>\n");
        htmlContent.append("    </tr>\n");
        htmlContent.append("</table>\n");
        
        // SECTION 3: Allowances
        double riceSubsidy = getRiceSubsidy();
        double phoneAllowance = getPhoneAllowance();
        double clothingAllowance = getClothingAllowance();
        double totalAllowances = getTotalAllowances();
        
        htmlContent.append("<table>\n");
        htmlContent.append("    <tr>\n");
        htmlContent.append("        <th colspan=\"2\" class=\"section-title\">ALLOWANCES</th>\n");
        htmlContent.append("    </tr>\n");
        htmlContent.append("    <tr>\n");
        htmlContent.append("        <td width=\"70%\">Rice Subsidy:</td>\n");
        htmlContent.append("        <td width=\"30%\" class=\"right-align\">").append(formatAsPhp(riceSubsidy)).append("</td>\n");
        htmlContent.append("    </tr>\n");
        htmlContent.append("    <tr>\n");
        htmlContent.append("        <td>Phone Allowance:</td>\n");
        htmlContent.append("        <td class=\"right-align\">").append(formatAsPhp(phoneAllowance)).append("</td>\n");
        htmlContent.append("    </tr>\n");
        htmlContent.append("    <tr>\n");
        htmlContent.append("        <td>Clothing Allowance:</td>\n");
        htmlContent.append("        <td class=\"right-align\">").append(formatAsPhp(clothingAllowance)).append("</td>\n");
        htmlContent.append("    </tr>\n");
        htmlContent.append("    <tr class=\"total-row\">\n");
        htmlContent.append("        <td>Total Allowances:</td>\n");
        htmlContent.append("        <td class=\"right-align\">").append(formatAsPhp(totalAllowances)).append("</td>\n");
        htmlContent.append("    </tr>\n");
        htmlContent.append("</table>\n");
        
        // SECTION 4: Deductions
        double sssDeduction = getSssDeduction();
        double philHealthDeduction = getPhilHealthDeduction();
        double pagIbigDeduction = getPagIbigDeduction();
        double withholdingTax = getWithholdingTax();
        double lateDeductions = getLateDeductions();
        double totalDeductions = getTotalDeductions();
        double taxableIncome = getTaxableIncome();
        
        htmlContent.append("<table>\n");
        htmlContent.append("    <tr>\n");
        htmlContent.append("        <th colspan=\"2\" class=\"section-title\">DEDUCTIONS</th>\n");
        htmlContent.append("    </tr>\n");
        
        // SSS Deduction
        htmlContent.append("    <tr>\n");
        htmlContent.append("        <td width=\"70%\">SSS:</td>\n");
        htmlContent.append("        <td width=\"30%\" class=\"right-align\">").append(formatAsPhp(sssDeduction)).append("</td>\n");
        htmlContent.append("    </tr>\n");
        htmlContent.append("    <tr class=\"calculation-note\">\n");
        htmlContent.append("        <td colspan=\"2\">Based on SSS contribution table</td>\n");
        htmlContent.append("    </tr>\n");
        
        // PhilHealth Deduction
        htmlContent.append("    <tr>\n");
        htmlContent.append("        <td>PhilHealth:</td>\n");
        htmlContent.append("        <td class=\"right-align\">").append(formatAsPhp(philHealthDeduction)).append("</td>\n");
        htmlContent.append("    </tr>\n");
        htmlContent.append("    <tr class=\"calculation-note\">\n");
        htmlContent.append("        <td colspan=\"2\">(Monthly Basic Salary × 3%) ÷ 2</td>\n");
        htmlContent.append("    </tr>\n");
        
        // Pag-IBIG Deduction - UPDATED to match the new calculation
        htmlContent.append("    <tr>\n");
        htmlContent.append("        <td>Pag-IBIG:</td>\n");
        htmlContent.append("        <td class=\"right-align\">").append(formatAsPhp(pagIbigDeduction)).append("</td>\n");
        htmlContent.append("    </tr>\n");
        htmlContent.append("    <tr class=\"calculation-note\">\n");
        htmlContent.append("        <td colspan=\"2\">2% of Basic Salary (Maximum of ₱100)</td>\n");
        htmlContent.append("    </tr>\n");
        
        // Taxable Income
        htmlContent.append("    <tr>\n");
        htmlContent.append("        <td>Taxable Income:</td>\n");
        htmlContent.append("        <td class=\"right-align\">").append(formatAsPhp(taxableIncome)).append("</td>\n");
        htmlContent.append("    </tr>\n");
        
        // Withholding Tax
        htmlContent.append("    <tr>\n");
        htmlContent.append("        <td>Withholding Tax:</td>\n");
        htmlContent.append("        <td class=\"right-align\">").append(formatAsPhp(withholdingTax)).append("</td>\n");
        htmlContent.append("    </tr>\n");
        
        // Add tax calculation explanation based on tax bracket
        htmlContent.append("    <tr class=\"calculation-note\">\n");
        htmlContent.append("        <td colspan=\"2\">").append(getTaxExplanation()).append("</td>\n");
        htmlContent.append("    </tr>\n");
        
        // Only include late deductions if there are any
        if (lateDeductions > 0) {
            htmlContent.append("    <tr>\n");
            htmlContent.append("        <td>Late/Absence Deductions:</td>\n");
            htmlContent.append("        <td class=\"right-align\">").append(formatAsPhp(lateDeductions)).append("</td>\n");
            htmlContent.append("    </tr>\n");
            htmlContent.append("    <tr class=\"calculation-note\">\n");
            htmlContent.append("        <td colspan=\"2\">Late Hours × Hourly Rate</td>\n");
            htmlContent.append("    </tr>\n");
        }
        
        // Total Deductions
        htmlContent.append("    <tr class=\"total-row\">\n");
        htmlContent.append("        <td>Total Deductions:</td>\n");
        htmlContent.append("        <td class=\"right-align\">").append(formatAsPhp(totalDeductions)).append("</td>\n");
        htmlContent.append("    </tr>\n");
        htmlContent.append("</table>\n");
        
        // SECTION 5: Final Net Pay
        double netPay = getNetMonthlyPay();
        
        htmlContent.append("<table>\n");
        htmlContent.append("    <tr class=\"final-total\">\n");
        htmlContent.append("        <td width=\"70%\">NET PAY:</td>\n");
        htmlContent.append("        <td width=\"30%\" class=\"right-align\">").append(formatAsPhp(netPay)).append("</td>\n");
        htmlContent.append("    </tr>\n");
        htmlContent.append("</table>\n");
        
        // Close out all the HTML tags
        htmlContent.append("</body>\n");
        htmlContent.append("</html>\n");

        // Set up the file path in Downloads folder
        String downloadsFolder = System.getProperty("user.home") + "/Downloads";
        String fileNamePDF = "Payslip_" + getEmployeeId() + "_" + period.replace(" ", "_") + ".pdf";

        // Make sure the directory exists
        File directory = new File(downloadsFolder);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Create the PDF file
        File pdfFile = new File(downloadsFolder, fileNamePDF);
        convertToPdf(htmlContent.toString(), pdfFile.getAbsolutePath());
    }

    /**
     * Get the current payroll month this payslip is for
     * @return 
     */
    @Override
    public YearMonth getPayrollMonth() {
        return this.payrollMonth;
    }

    /**
     * Set which month this payslip is for
     * @param payrollMonth
     */
    @Override
    public void setPayrollMonth(YearMonth payrollMonth) {
        this.payrollMonth = payrollMonth;
        super.setPayrollMonth(payrollMonth); // Update parent class too
    }
    
    /**
     * Creates a unique filename by adding a timestamp
     */
    private String makeUniqueFileName(String originalFileName) {
        // Extract the base name and extension
        String baseName = originalFileName;
        String extension = "";
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = originalFileName.substring(0, dotIndex);
            extension = originalFileName.substring(dotIndex);
        }
        
        // Add timestamp to make it unique
        String timestamp = String.valueOf(System.currentTimeMillis() % 10000);
        return baseName + "_" + timestamp + extension;
    }
    
    /**
     * Converts HTML content to a PDF file using iText
     */
    private void convertToPdf(String htmlContent, String fileNamePDF) throws IOException, DocumentException {
        Document document = new Document(PageSize.A4);
        PdfWriter writer = null;
        int retryCount = 0;
        boolean success = false;
        
        // Add a timestamp to make the filename unique
        String originalFileName = fileNamePDF;
        fileNamePDF = makeUniqueFileName(originalFileName);
        
        while (!success && retryCount < 3) {
            try {
                // Set up the PDF writer
                writer = PdfWriter.getInstance(document, new FileOutputStream(fileNamePDF));
                document.open();

                // Set up CSS for styling the PDF
                CSSResolver cssResolver = new StyleAttrCSSResolver();
                CssFile cssFile = XMLWorkerHelper.getCSS(new ByteArrayInputStream(
                        "body { font-family: Arial, sans-serif; } ".getBytes()));
                cssResolver.addCss(cssFile);

                // Set up the HTML context and font provider
                XMLWorkerFontProvider fontProvider = new XMLWorkerFontProvider();
                CssAppliers cssAppliers = new CssAppliersImpl(fontProvider);
                HtmlPipelineContext htmlContext = new HtmlPipelineContext(cssAppliers);
                htmlContext.setTagFactory(Tags.getHtmlTagProcessorFactory());

                // Set up the processing pipeline
                PdfWriterPipeline pdf = new PdfWriterPipeline(document, writer);
                HtmlPipeline html = new HtmlPipeline(htmlContext, pdf);
                CssResolverPipeline css = new CssResolverPipeline(cssResolver, html);

                // Convert the HTML to PDF
                XMLWorker worker = new XMLWorker(css, true);
                XMLParser p = new XMLParser(worker);
                p.parse(new StringReader(htmlContent));
                success = true;
            } catch (IOException e) {
                // If file is in use, try a different filename
                if (e.getMessage().contains("process cannot access the file")) {
                    retryCount++;
                    fileNamePDF = makeUniqueFileName(originalFileName);
                    System.out.println("File in use, trying alternate filename: " + fileNamePDF);
                } else {
                    // For other IO exceptions, log and re-throw
                    System.err.println("IO Exception during PDF conversion: " + e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            } catch (DocumentException e) {
                System.err.println("Document Exception during PDF conversion: " + e.getMessage());
                e.printStackTrace();
                throw e;
            } finally {
                // Make sure to close everything properly
                if (document != null && document.isOpen()) {
                    document.close();
                }
                if (writer != null) {
                    writer.close();
                }
            }
        }
        
        if (!success) {
            throw new IOException("Failed to create PDF after multiple attempts. Please close any open PDF files and try again.");
        } else {
            System.out.println("Payslip successfully saved as: " + fileNamePDF);
        }
    }
}