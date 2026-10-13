package com.chngy.jobscraper.Service;

import com.chngy.jobscraper.DTO.ListingDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ScorerTest {

    private Scorer scorer;

    String controlledPositiveJD = """
            Java Spring Boot React PHP javascript
            """;

    String controlledNegativeJD = """
            Architect Mentor PHP java leader
            """;

    String controlledMixedJD = """
            React native boss man go work
            """;

    String staffSWEJD = """
                We are seeking a highly skilled and experienced Staff Software Engineer to join our
                team in developing innovative software solutions that enable the organisation to effectively 
                manage information and workflows. For this role, you will be working with highly motivated 
                and talented peers consisting of Software engineers, DevOps engineers and UI/UX designers. 
                As a Staff Software Engineer, you are responsible for overseeing and mentoring teams of 
                technical personnel to develop and deploy secure and high-quality software product through 
                adoption of good industrial practices. You will work closely with management team to deliver 
                the right software products to realise business strategies and needs. We are looking for 
                passionate and talented people in building right solutions and cultivating good software 
                engineering practices within the team. What you will be working on: • Work with stakeholders 
                to understand business requirements and design technically sound software products that meet 
                the business goals. • Lead the design, development and operation of software products in 
                organisation units. • Provide technical guidance and mentorship to the software teams and 
                other stakeholders to help them make the best possible technical decisions. • Ensure the 
                results and quality of the different software solutions and projects implemented in the 
                organisation. • Collaborate closely with other software product teams to integrate systems 
                across business function seamlessly. • Be a technical mentor to help junior engineers grow 
                in their engineering career. What we are looking for: • Background in Engineering or Computer 
                Science, with at least 6 years of relevant experience. • Strong knowledge of software 
                architecture (e.g. Micro-services, Event-drive, Cloud-native architecture) and design 
                patterns (e.g. RESTful API, MVC, MVVM design). • Well-versed and proficient in software 
                design and implementation using modern tools (e.g. SonarQube, Kubernetes, GitLab) and 
                frameworks (e.g. MERN, Spring Boot, React Native) to develop high quality software efficiently. 
                • Strong understanding of software security best practices to actively identify and mitigate 
                potential security risks and vulnerabilities in software systems. • Experience with Agile 
                development methodologies, including Scrum or Kanban. • Strong communicator that can explain 
                complex technical concepts to both technical and non-technical colleagues. • At least 3 years 
                of experience in technical leadership role leading software teams and setting technical 
                direction. As CSIT is an agency under the Ministry of Defence (Singapore), only Singapore 
                Citizens will be considered. About Centre for Strategic Infocomm Technologies The Centre for 
                Strategic Infocomm Technologies is the lead digital technology agency in the Ministry of 
                Defence, and a member of the Defence Technology Community. Our technical focus areas include 
                Cybersecurity, Data Analytics, Software Engineering, and Cloud Infrastructure and Services.
                 We conduct applied research and develop customised digital solutions to support national 
                 missions such as cyber defence and counter-terrorism. Please visit our website 
                 (https://www.csit.gov.sg/) for more info on what we do, for scholarship/internship/career 
                 opportunities and more! React Java Spring
                """;

    String irrelevantJD = """
                What the role is: HTX is Singapore's Science and Technology agency that brings together 
                diverse scientific and engineering capabilities to develop transformative, operationally 
                ready solutions for public safety. As a statutory board under the Ministry of Home Affairs, 
                HTX works at the forefront of science and technology to empower the Home Team with cutting-edge 
                capabilities. Guided by our mission to amplify, augment and accelerate the Home Team's advantage, 
                we are committed to keeping Singapore the safest place on planet Earth. As a Digital Forensic 
                Examiner, you will be part of a high-tech team supporting the Home Team in keeping 
                Singapore safe and secure. Digital forensics plays an important role in generating leads 
                for investigators to track down criminals and uncover digital evidence to support the 
                prosecution of these criminals. What you will be working on: - Support criminal investigations 
                by assisting in seizure and conduct of forensic examination on digital devices to extract 
                critical electronic evidence. - Support digital forensic investigations, involving interaction 
                with large datasets over multiple sources, operating systems and/or networks. - Analyse 
                advanced and/or proprietary operating systems, software applications and internet 
                platforms - Stay on top of emerging trends in digital and information forensics and 
                devise strategies to enhance operational capabilities to meet the challenges. - Performs 
                any other digital forensic-related duties as assigned. What we are looking for: - 
                Tertiary qualification in Information Technology, Computer Science, Computer Engineering, 
                Electrical and Electronic Engineering or a related discipline - Passion in electronic hardware, 
                coding/scripting and cloud technology would be an advantage - Motivated and able to work 
                independently and/or as part of a team - Good analytical and communication abilities All new 
                appointees will be provided with relevant training and appointed on a two-year contract in the 
                first instance. We wish to inform that only shortlisted candidates will be notified within 4 week 
                upon closing of the advertisement. As part of the shortlisting process for this role, you may be 
                required to complete a medical declaration and/or undergo further assessment. #LI-JO1 About Home 
                Team Science and Technology Agency (HTX) HTX is the world’s first Science and Technology agency 
                that integrates a diverse range of scientific and engineering capabilities to innovate and deliver 
                transformative and operationally-ready solutions for homeland security. As a statutory board of the 
                Ministry of Home Affairs and integral to the Home Team, HTX works at the forefront of science and 
                technology to empower Singapore’s frontline of security. Senior Our shared mission is to amplify, augment 
                and accelerate the Home Team’s advantage and secure Singapore as the safest place on planet earth.
                """;

    ListingDTO jobWithIrrelevantJD = ListingDTO.builder().id("1").jobDescription(irrelevantJD).build();
    ListingDTO jobWithStaffSWEJD = ListingDTO.builder().id("2").jobDescription(staffSWEJD).build();

    @BeforeEach
    void setUp() {
        // 1. Create a raw instance of your service
        scorer = new Scorer();

        // 2. Mock and inject the private @Value fields manually
        ReflectionTestUtils.setField(scorer, "NUMBER_OF_RESULTS", 5);
        ReflectionTestUtils.setField(scorer, "POSITIVE_SCORER_ITEMS", List.of("java", "spring", "react", "javascript"));
        // Double positive is javascript for keywords java and javascript
        ReflectionTestUtils.setField(scorer, "NEGATIVE_SCORER_ITEMS", List.of("lead", "php", "react native", "leader"));
        // Double negative is leader for keywords lead and leader
        // Mixed is react native for keywords react and react native
    }

    @Test
    public void scorerGivenPositiveJDItWillReturnNumberOfHits() {

        List<ListingDTO> listings = new ArrayList<>();
        listings.add(jobWithStaffSWEJD);
        List<ListingDTO> results = scorer.score(listings);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.getFirst().score()>0);
    }

    @Test
    public void scorerGivenIrrelevantJDItWillReturnNumberOfHits() {
        List<ListingDTO> listings = new ArrayList<>();
        listings.add(jobWithIrrelevantJD);
        List<ListingDTO> results = scorer.score(listings);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(0, results.getFirst().score());
    }

    @Test
    public void scorerGivenNegativeControlledJDItWillReturnNegativeHits() {
        List<ListingDTO> listings = new ArrayList<>();
        listings.add(ListingDTO.builder().jobDescription(controlledNegativeJD).build());
        List<ListingDTO> results = scorer.score(listings);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(-1, results.getFirst().score());
    }

    // 4-1 since java, react , spring and javascript minus PHP
    @Test
    public void scorerGivenDoublePositiveCountWillNotDoubleCount() {
        List<ListingDTO> listings = new ArrayList<>();
        listings.add(ListingDTO.builder().jobDescription(controlledPositiveJD).build());
        List<ListingDTO> results = scorer.score(listings);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(3, results.getFirst().score());
    }

    // 1-2 : java minus leader minus PHP
    @Test
    public void scorerGivenDoubleNegativeCountWillNotDoubleCount() {
        List<ListingDTO> listings = new ArrayList<>();
        listings.add(ListingDTO.builder().jobDescription(controlledNegativeJD).build());
        List<ListingDTO> results = scorer.score(listings);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(-1, results.getFirst().score());
    }

    // -1 since react native should be the only one counted
    @Test
    public void scorerGivenMixedCountWillNettOff() {
        List<ListingDTO> listings = new ArrayList<>();
        listings.add(ListingDTO.builder().jobDescription(controlledMixedJD).build());
        List<ListingDTO> results = scorer.score(listings);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(-1, results.getFirst().score());
    }
}
